# W-E10 supplementary probe: Windows Credential Manager write/read/delete (ASCII only).
$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public class CredApi2 {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct CREDENTIAL {
        public int Flags;
        public int Type;
        public string TargetName;
        public string Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public int CredentialBlobSize;
        public IntPtr CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public IntPtr Attributes;
        public string TargetAlias;
        public string UserName;
    }
    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern bool CredWrite(ref CREDENTIAL cred, int flags);
    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern bool CredRead(string target, int type, int flags, out IntPtr credential);
    [DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern bool CredDelete(string target, int type, int flags);
    [DllImport("advapi32.dll", SetLastError = true)]
    public static extern void CredFree(IntPtr buffer);
}
"@

$target = 'ReaderWinProbe/we10'
$secret = 'we10-credman-secret'
$result = [ordered]@{
    target = $target; writeOk = $false; readOk = $false; secretMatched = $false
    deleteOk = $false; readAfterDeleteOk = $false; errors = @()
}

# 1) write
try {
    $blob = [System.Runtime.InteropServices.Marshal]::StringToCoTaskMemUni($secret)
    $cred = New-Object CredApi2+CREDENTIAL
    $cred.Type = 1
    $cred.TargetName = $target
    $cred.UserName = 'reader-test'
    $cred.CredentialBlob = $blob
    $cred.CredentialBlobSize = $secret.Length * 2
    $cred.Persist = 1
    $result.writeOk = [CredApi2]::CredWrite([ref]$cred, 0)
    if (-not $result.writeOk) { $result.errors += "write:" + [Runtime.InteropServices.Marshal]::GetLastWin32Error() }
    [System.Runtime.InteropServices.Marshal]::ZeroFreeCoTaskMemUnicode($blob)
} catch { $result.errors += "write-ex:" + $_.Exception.Message }

# 2) read
try {
    $ptr = [IntPtr]::Zero
    $ok = [CredApi2]::CredRead($target, 1, 0, [ref]$ptr)
    $result.readOk = $ok
    if ($ok -and $ptr -ne [IntPtr]::Zero) {
        $read = [System.Runtime.InteropServices.Marshal]::PtrToStructure($ptr, [type][CredApi2+CREDENTIAL])
        if ($read.CredentialBlobSize -gt 0) {
            $text = [System.Runtime.InteropServices.Marshal]::PtrToStringUni($read.CredentialBlob, [int]($read.CredentialBlobSize / 2))
            $result.secretMatched = ($text -eq $secret)
        }
        [CredApi2]::CredFree($ptr)
    } elseif (-not $ok) {
        $result.errors += "read:" + [Runtime.InteropServices.Marshal]::GetLastWin32Error()
    }
} catch { $result.errors += "read-ex:" + $_.Exception.Message }

# 3) delete
try {
    $result.deleteOk = [CredApi2]::CredDelete($target, 1, 0)
    if (-not $result.deleteOk) { $result.errors += "delete:" + [Runtime.InteropServices.Marshal]::GetLastWin32Error() }
} catch { $result.errors += "delete-ex:" + $_.Exception.Message }

# 4) read after delete
try {
    $ptr2 = [IntPtr]::Zero
    $still = [CredApi2]::CredRead($target, 1, 0, [ref]$ptr2)
    $result.readAfterDeleteOk = $still
    if ($still -and $ptr2 -ne [IntPtr]::Zero) { [CredApi2]::CredFree($ptr2) }
} catch { $result.errors += "read-after-ex:" + $_.Exception.Message }

$result | ConvertTo-Json -Depth 3
