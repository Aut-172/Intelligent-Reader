package reader.windowsmin.app.task

import java.io.File

/**
 * W-E07 crash/restart probe.
 * mode=crash : enqueue tasks, claim one with a short lease, then hard-halt the JVM.
 * mode=verify: reopen the same DB, recover expired leases, claim again and print state.
 */
fun main(args: Array<String>) {
    require(args.size >= 2) { "usage: CrashProbeKt <crash|verify> <dbPath>" }
    val mode = args[0]
    val db = File(args[1])
    if (mode == "dump") {
        java.sql.DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT task_id,state,attempts,lease_until FROM tasks ORDER BY task_id").use { rs ->
                    while (rs.next()) {
                        println("ROW ${rs.getString(1)} ${rs.getString(2)} attempts=${rs.getInt(3)} lease=${rs.getObject(4)}")
                    }
                }
            }
        }
        return
    }
    if (mode == "power") {
        val progress = File(args.getOrNull(2) ?: "power-progress.txt")
        val store = SqliteTaskStore(db)
        var i = 0
        while (true) {
            i++
            val id = "p-$i"
            store.enqueue(id, "index", """{"i":$i}""")
            val t = store.claimNext(System.currentTimeMillis(), 30_000)
            if (t != null) store.complete(t.taskId, System.currentTimeMillis())
            if (i % 50 == 0) progress.appendText("count=$i pending=${store.pendingCount()}\n")
        }
    }
    if (mode == "power-verify") {
        val out = File(args.getOrNull(2) ?: "power-verify.txt")
        if (!out.exists()) out.writeText("")
        val integrity = java.sql.DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery("PRAGMA integrity_check").use { rs -> if (rs.next()) rs.getString(1) else "?" }
            }
        }
        SqliteTaskStore(db).use { store ->
            val now = System.currentTimeMillis() + 60_000
            val recovered = store.recoverExpiredLeases(now)
            var drained = 0
            while (true) {
                val t = store.claimNext(now, 30_000) ?: break
                store.complete(t.taskId, now)
                drained++
            }
            val counts = mutableMapOf<String, Int>()
            java.sql.DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
                c.createStatement().use { st ->
                    st.executeQuery("SELECT state, COUNT(*) FROM tasks GROUP BY state").use { rs ->
                        while (rs.next()) counts[rs.getString(1)] = rs.getInt(2)
                    }
                }
            }
            val line = "integrity=$integrity recovered=$recovered drained=$drained counts=$counts pending=${store.pendingCount()}"
            out.appendText(line + "\n")
            println(line)
        }
        return
    }
    if (mode == "seed") {
        val n = args.getOrNull(2)?.toIntOrNull() ?: 100
        SqliteTaskStore(db).use { store ->
            var added = 0
            for (i in 1..n) if (store.enqueue("task-$i", "index", """{"doc":$i}""")) added++
            println("SEED added=$added pending=${store.pendingCount()}")
        }
        return
    }
    if (mode == "worker-crash") {
        val id = args.getOrNull(2) ?: "wc"
        val out = File(args.getOrNull(3) ?: "worker-$id.txt")
        out.writeText("")
        val lease = args.getOrNull(4)?.toLongOrNull() ?: 30_000L
        val store = SqliteTaskStore(db)
        val t = store.claimNext(System.currentTimeMillis(), lease)
        if (t != null) {
            out.appendText(t.taskId + "\n")
            out.appendText("CRASH_HELD " + t.taskId + "\n")
        } else {
            out.appendText("CRASH_NONE\n")
        }
        println("WORKER-CRASH $id claimed=${t?.taskId}")
        System.out.flush()
        Runtime.getRuntime().halt(7)
    }
    if (mode == "worker") {
        val id = args.getOrNull(2) ?: "w0"
        val out = File(args.getOrNull(3) ?: "worker-$id.txt")
        val lease = args.getOrNull(4)?.toLongOrNull() ?: 30_000L
        out.writeText("")
        SqliteTaskStore(db).use { store ->
            var claimed = 0
            var idle = 0
            var loops = 0
            while (loops++ < 20_000) {
                try {
                    val t = store.claimNext(System.currentTimeMillis(), lease)
                    if (t == null) {
                        idle++
                        if (idle >= 20) break
                        Thread.sleep(50)
                        continue
                    }
                    idle = 0
                    out.appendText(t.taskId + "\n")
                    Thread.sleep(5)
                    store.complete(t.taskId, System.currentTimeMillis())
                    claimed++
                } catch (e: Exception) {
                    out.appendText("ERR " + (e.message ?: e.javaClass.simpleName) + "\n")
                    Thread.sleep(100)
                }
            }
            println("WORKER $id claimed=$claimed pending=${store.pendingCount()}")
        }
        return
    }
    if (mode == "check") {
        val files = args.drop(2)
        val ids = files.flatMap { f -> File(f).readLines().filter { !it.startsWith("ERR") && !it.startsWith("CRASH_") && it.isNotBlank() } }
        val dups = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
        java.sql.DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT state, COUNT(*) FROM tasks GROUP BY state").use { rs ->
                    while (rs.next()) println("DB ${rs.getString(1)}=${rs.getInt(2)}")
                }
            }
        }
        println("CHECK workers=${files.size} claims=${ids.size} distinct=${ids.toSet().size} duplicates=${dups.size}")
        if (dups.isNotEmpty()) println("CHECK dup_ids=" + dups.entries.take(5).joinToString { it.key + ":" + it.value })
        return
    }
    if (mode == "crash") {
        val store = SqliteTaskStore(db)
        for (i in 1..5) store.enqueue("task-$i", "index", """{"doc":$i}""")
        val claimed = store.claimNext(System.currentTimeMillis(), 30_000)
        println("CRASH claimed=${claimed?.taskId} state=${claimed?.state} lease=${claimed?.leaseUntilEpochMs}")
        println("CRASH pending=${store.pendingCount()}")
        System.out.flush()
        Runtime.getRuntime().halt(7)
    } else {
        SqliteTaskStore(db).use { store ->
            val offset = args.getOrNull(2)?.toLongOrNull() ?: 31_000L
            val now = System.currentTimeMillis() + offset
            val recovered = store.recoverExpiredLeases(now)
            println("VERIFY recovered=$recovered pending=${store.pendingCount()}")
            val again = store.claimNext(now, 30_000)
            println("VERIFY claimed=${again?.taskId} attempts=${again?.attempts} state=${again?.state}")
            if (again != null) store.complete(again.taskId, now)
            val duplicate = store.enqueue("task-1", "index", """{"doc":1}""")
            println("VERIFY duplicate_enqueue=$duplicate pending=${store.pendingCount()}")
            var seen = 0
            while (true) {
                val t = store.claimNext(now, 30_000) ?: break
                seen++
                store.complete(t.taskId, now)
            }
            println("VERIFY drained=$seen pending=${store.pendingCount()}")
        }
    }
}
