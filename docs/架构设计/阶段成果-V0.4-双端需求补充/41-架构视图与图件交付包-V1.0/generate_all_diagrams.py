import os
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.patches as patches

# Set font family
plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans', 'Arial']
plt.rcParams['axes.unicode_minus'] = False

OUTPUT_DIR_SVG = r"D:\北航事务\大三上\软分\软分项目\Intelligent-Reader\docs\架构设计\阶段成果-V0.4-双端需求补充\41-架构视图与图件交付包-V1.0\导出图-SVG"
OUTPUT_DIR_PNG = r"D:\北航事务\大三上\软分\软分项目\Intelligent-Reader\docs\架构设计\阶段成果-V0.4-双端需求补充\41-架构视图与图件交付包-V1.0\导出图-PNG"
OUTPUT_DIR_PDF = r"D:\北航事务\大三上\软分\软分项目\Intelligent-Reader\docs\架构设计\阶段成果-V0.4-双端需求补充\41-架构视图与图件交付包-V1.0\导出图-PDF"

os.makedirs(OUTPUT_DIR_SVG, exist_ok=True)
os.makedirs(OUTPUT_DIR_PNG, exist_ok=True)
os.makedirs(OUTPUT_DIR_PDF, exist_ok=True)

def draw_header(ax, title, subtitle, status="Proposed", meta_info=""):
    ax.text(0.5, 0.965, title, fontsize=14, fontweight='bold', ha='center', va='top', color='#1A237E')
    ax.text(0.5, 0.935, subtitle, fontsize=10, ha='center', va='top', color='#37474F')
    
    # Status badge
    bg_color = '#C8E6C9' if status == 'Accepted' else '#FFE0B2'
    border_color = '#2E7D32' if status == 'Accepted' else '#E65100'
    text_color = '#1B5E20' if status == 'Accepted' else '#BF360C'
    
    badge = patches.FancyBboxPatch((0.83, 0.925), 0.14, 0.035, boxstyle="round,pad=0.01", 
                                  facecolor=bg_color, edgecolor=border_color, lw=1.5)
    ax.add_patch(badge)
    ax.text(0.90, 0.942, f"状态: {status}", fontsize=9, fontweight='bold', ha='center', va='center', color=text_color)
    
    if meta_info:
        ax.text(0.03, 0.935, meta_info, fontsize=8, ha='left', va='top', color='#546E7A')

def save_fig(fig, base_name):
    svg_path = os.path.join(OUTPUT_DIR_SVG, f"{base_name}.svg")
    png_path = os.path.join(OUTPUT_DIR_PNG, f"{base_name}.png")
    pdf_path = os.path.join(OUTPUT_DIR_PDF, f"{base_name}.pdf")
    fig.savefig(svg_path, bbox_inches='tight')
    fig.savefig(png_path, dpi=200, bbox_inches='tight')
    fig.savefig(pdf_path, bbox_inches='tight')
    plt.close(fig)
    print(f"Generated: {base_name}")

# ==========================================
# Generic Sequence Diagram Drawer
# ==========================================
def draw_sequence_diagram(base_name, title, subtitle, participants, steps, notes=None, status="Proposed", meta_info=""):
    # participants: list of (label, subtext, color)
    # steps: list of dict(step=1, frm=0, to=1, text="msg", is_ret=False, alt_label=None)
    # notes: list of dict(y_pos=0.1, text="...", color="#FFF9C4")
    
    num_p = len(participants)
    num_steps = len(steps)
    
    fig_height = max(11, 2.5 + num_steps * 0.45)
    fig, ax = plt.subplots(figsize=(15, fig_height))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    
    draw_header(ax, title, subtitle, status=status, meta_info=meta_info)
    
    # Calculate participant X positions
    p_xs = [0.06 + i * (0.88 / (num_p - 1)) for i in range(num_p)]
    
    # Draw participant boxes and lifelines
    y_top = 0.88
    y_bottom = 0.08
    
    for i, (name, subtext, color) in enumerate(participants):
        x = p_xs[i]
        box_w = 0.88 / num_p * 0.85
        box = patches.FancyBboxPatch((x - box_w/2, y_top - 0.04), box_w, 0.045, boxstyle="round,pad=0.008",
                                     facecolor=color, edgecolor='#37474F', lw=1.5)
        ax.add_patch(box)
        ax.text(x, y_top - 0.015, name, fontsize=8.5, fontweight='bold', ha='center', va='center', color='#263238')
        if subtext:
            ax.text(x, y_top - 0.032, subtext, fontsize=7.5, ha='center', va='center', color='#455A64')
            
        # Lifeline (dashed)
        ax.plot([x, x], [y_top - 0.045, y_bottom], color='#B0BEC5', linestyle='--', lw=1.2, zorder=1)
        
        # Bottom box
        box_b = patches.FancyBboxPatch((x - box_w/2, y_bottom - 0.035), box_w, 0.03, boxstyle="round,pad=0.008",
                                       facecolor=color, edgecolor='#37474F', lw=1.2)
        ax.add_patch(box_b)
        ax.text(x, y_bottom - 0.02, name, fontsize=7.5, ha='center', va='center', color='#263238')

    # Draw steps
    step_y_step = (y_top - 0.06 - y_bottom) / (num_steps + 1)
    curr_y = y_top - 0.065
    
    for s in steps:
        curr_y -= step_y_step
        frm_idx = s['frm']
        to_idx = s['to']
        text = s['text']
        is_ret = s.get('is_ret', False)
        alt_label = s.get('alt_label', None)
        is_self = (frm_idx == to_idx)
        
        x_from = p_xs[frm_idx]
        x_to = p_xs[to_idx]
        
        if alt_label:
            # Draw an alt boundary box
            alt_box = patches.FancyBboxPatch((0.03, curr_y - 0.015), 0.94, step_y_step * 0.9, 
                                             boxstyle="round,pad=0.005", facecolor='#F5F5F5', 
                                             edgecolor='#78909C', lw=1, linestyle=':')
            ax.add_patch(alt_box)
            ax.text(0.04, curr_y + 0.005, alt_label, fontsize=7.5, fontweight='bold', color='#37474F', 
                    bbox=dict(boxstyle="square,pad=0.2", facecolor='#CFD8DC', edgecolor='none'))
            curr_y -= 0.02
        
        if is_self:
            # Loopback arrow
            loop_w = 0.03
            loop_h = step_y_step * 0.4
            ax.plot([x_from, x_from + loop_w, x_from + loop_w, x_from],
                    [curr_y, curr_y, curr_y - loop_h, curr_y - loop_h],
                    color='#37474F', lw=1.2)
            ax.annotate("", xy=(x_from, curr_y - loop_h), xytext=(x_from + 0.008, curr_y - loop_h),
                        arrowprops=dict(arrowstyle="->", lw=1.2, color='#37474F'))
            ax.text(x_from + loop_w + 0.005, curr_y - loop_h / 2, f"{s['step']}. {text}", 
                    fontsize=7.5, ha='left', va='center', color='#1A237E')
            curr_y -= loop_h * 0.5
        else:
            style = "->"
            line_style = "--" if is_ret else "-"
            arrow_color = '#546E7A' if is_ret else '#0D47A1'
            
            ax.annotate("", xy=(x_to, curr_y), xytext=(x_from, curr_y),
                        arrowprops=dict(arrowstyle=style, linestyle=line_style, lw=1.3, color=arrow_color))
            
            # Message text
            mid_x = (x_from + x_to) / 2
            ax.text(mid_x, curr_y + 0.008, f"{s['step']}. {text}", fontsize=7.5, 
                    ha='center', va='bottom', color='#0D47A1' if not is_ret else '#37474F',
                    bbox=dict(boxstyle="square,pad=0.15", facecolor='#FFFFFF', edgecolor='none', alpha=0.85))

    # Draw bottom notes
    if notes:
        for n in notes:
            n_box = patches.FancyBboxPatch((0.03, 0.01), 0.94, 0.05, boxstyle="round,pad=0.005",
                                           facecolor=n.get('color', '#FFF9C4'), edgecolor='#FBC02D', lw=1.2)
            ax.add_patch(n_box)
            ax.text(0.5, 0.035, n['text'], fontsize=8, ha='center', va='center', color='#F57F17')

    save_fig(fig, base_name)

# ==========================================
# 1. ARCH-01 系统上下文与信任边界图
# ==========================================
def gen_arch_01():
    fig, ax = plt.subplots(figsize=(15, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    
    draw_header(ax, "图 ARCH-01 系统上下文与信任边界图", 
                "System Context & Trust Boundary Diagram (UML 上下文协作视图)", 
                status="Proposed", 
                meta_info="关联决策: D-01, D-05, D-09, D-15, D-17, D-23, D-24 | 质量场景: QA-29, QA-36")
    
    # User Box
    user_box = patches.FancyBboxPatch((0.03, 0.45), 0.12, 0.2, boxstyle="round,pad=0.01", facecolor='#E1F5FE', edgecolor='#0288D1', lw=2)
    ax.add_patch(user_box)
    ax.text(0.09, 0.58, "个人用户\n(Personal User)\n\n单用户本地工作区\n无团队/协作角色", fontsize=10, ha='center', va='center', color='#01579B', fontweight='bold')
    
    # Client Sandbox Box (TB-1)
    client_box = patches.FancyBboxPatch((0.2, 0.15), 0.35, 0.72, boxstyle="round,pad=0.01", facecolor='#F1F8E9', edgecolor='#558B2F', lw=2.5)
    ax.add_patch(client_box)
    ax.text(0.375, 0.84, "客户端受信任环境 (Android 12+ / Win 11) [信任边界 TB-1: 私有沙箱]", fontsize=11, fontweight='bold', ha='center', color='#33691E')
    
    # Inside Client: UI & Logic
    c_ui = patches.FancyBboxPatch((0.22, 0.58), 0.31, 0.22, boxstyle="round,pad=0.01", facecolor='#FFFFFF', edgecolor='#7CB342', lw=1.5)
    ax.add_patch(c_ui)
    ax.text(0.375, 0.76, "智能阅读器客户端应用 (双端分立UI + KMP共享核心)", fontsize=9.5, fontweight='bold', ha='center', color='#2E7D32')
    ax.text(0.375, 0.67, "• Compose (Android) / Win Desktop UI\n• 平台文档组件 (PdfRenderer / Win.Data.Pdf)\n• KMP 共享领域、版本规则与同步状态机\n• 本地混合检索 (BM25 + 中文向量模型)", fontsize=8.5, ha='center', va='center', color='#1B5E20')
    
    # Inside Client: Local Data Store
    c_data = patches.FancyBboxPatch((0.22, 0.2), 0.31, 0.33, boxstyle="round,pad=0.01", facecolor='#DCEDC8', edgecolor='#33691E', lw=2, linestyle='--')
    ax.add_patch(c_data)
    ax.text(0.375, 0.49, "客户端专有本地资产 [绝不离开设备]", fontsize=10, fontweight='bold', ha='center', color='#1B5E20')
    ax.text(0.375, 0.35, "【严禁上传云端】\n1. 文档本体 (PDF/DOCX/MD 原文件)\n2. 页面位图、解析树与 OCR 识别结果\n3. 本地 BM25 全文索引库与向量资产\n4. 本地未外发笔记与阅读排版缓存\n5. SAF 持久授权 URI / Windows 本地文件路径", fontsize=8.5, ha='center', va='center', color='#2E7D32')

    # Server Box (TB-2)
    server_box = patches.FancyBboxPatch((0.6, 0.25), 0.22, 0.62, boxstyle="round,pad=0.01", facecolor='#EDE7F6', edgecolor='#512DA8', lw=2.5)
    ax.add_patch(server_box)
    ax.text(0.71, 0.84, "项目服务端 (云中立/一期可同机)", fontsize=11, fontweight='bold', ha='center', color='#311B92')
    ax.text(0.71, 0.81, "[信任边界 TB-2: HTTPS 网络边界]", fontsize=9, ha='center', color='#5E35B1')
    
    # Inside Server Components
    srv_comp = patches.FancyBboxPatch((0.615, 0.55), 0.19, 0.23, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.5)
    ax.add_patch(srv_comp)
    ax.text(0.71, 0.74, "三个粗粒度逻辑服务单元", fontsize=9.5, fontweight='bold', ha='center', color='#4527A0')
    ax.text(0.71, 0.65, "1. 身份与同步单元 (Sync Svc)\n   REST push/pull, revision, cursor\n2. AI/RAG 网关单元 (AI Gateway)\n   最少证据校验, SSE 流式, 轮询\n3. 任务与审计单元 (Worker)\n   PostgreSQL lease 抢占调度", fontsize=8, ha='center', va='center', color='#311B92')
    
    srv_db = patches.FancyBboxPatch((0.615, 0.28), 0.19, 0.23, boxstyle="round,pad=0.008", facecolor='#D1C4E9', edgecolor='#311B92', lw=1.5)
    ax.add_patch(srv_db)
    ax.text(0.71, 0.47, "PostgreSQL 关系数据库", fontsize=9.5, fontweight='bold', ha='center', color='#311B92')
    ax.text(0.71, 0.37, "【服务端最小结构化数据】\n• 账号凭据与设备会话\n• 文档身份哈希 (不可逆, 无正文)\n• 同步进度、笔记与冲突副本\n• 不可变 SyncOperation 变更流\n• 异步任务表与内容无关审计", fontsize=8, ha='center', va='center', color='#1A237E')

    # External LLM Box (TB-3)
    ext_box = patches.FancyBboxPatch((0.86, 0.4), 0.12, 0.35, boxstyle="round,pad=0.01", facecolor='#FFF3E0', edgecolor='#E65100', lw=2.5)
    ax.add_patch(ext_box)
    ax.text(0.92, 0.71, "受控外部模型供应商", fontsize=10, fontweight='bold', ha='center', color='#BF360C')
    ax.text(0.92, 0.67, "[信任边界 TB-3: 隔离边界]", fontsize=8.5, ha='center', color='#D84315')
    ax.text(0.92, 0.53, "OpenAI 官方端点\n(Responses API 候选)\n\ngpt-5.4-mini-2026-03-17\n(固定快照待正式验证)\n\n仅接收单次授权 Prompt\n与裁剪后的最少证据片段", fontsize=8, ha='center', va='center', color='#E65100')

    # Connecting Arrows
    ax.annotate("", xy=(0.2, 0.55), xytext=(0.15, 0.55), arrowprops=dict(arrowstyle="->", lw=2, color='#0288D1'))
    ax.text(0.175, 0.57, "本地交互\n授权/阅读", fontsize=8, ha='center', color='#0288D1')
    
    ax.annotate("", xy=(0.6, 0.65), xytext=(0.55, 0.65), arrowprops=dict(arrowstyle="->", lw=2, color='#512DA8'))
    ax.text(0.575, 0.67, "HTTPS (TLS 1.3)\n1. 结构化同步变更\n2. 授权最少证据包", fontsize=8, ha='center', color='#512DA8')
    
    ax.annotate("", xy=(0.86, 0.58), xytext=(0.82, 0.58), arrowprops=dict(arrowstyle="->", lw=2, color='#E65100'))
    ax.text(0.84, 0.61, "ModelAdapter\nResponses API", fontsize=8, ha='center', color='#E65100')

    # Bottom Callout Box
    bot_box = patches.FancyBboxPatch((0.03, 0.03), 0.94, 0.09, boxstyle="round,pad=0.01", facecolor='#FFEBEE', edgecolor='#C62828', lw=1.5)
    ax.add_patch(bot_box)
    ax.text(0.5, 0.075, "核心架构守则与负向约束 (ADR-011 至 ADR-035 强制基线):\n1. 本地优先：文档本体、解析树、高精坐标与全文/向量混合索引 100% 留存各端本地私有沙箱，绝对禁止上传服务端！\n2. 云端最少化：服务端 PostgreSQL 仅持有不可逆哈希、阅读进度、笔记文本及游标变更流；无团队空间、无公开分享服务；无 Web 客户端。\n3. AI 外发逐次授权：向外部模型仅提供经用户逐文件显式同意、严格裁剪后的最少证据包；返回引用必须经由客户端白名单核验。", fontsize=8.5, ha='center', va='center', color='#B71C1C')

    save_fig(fig, "ARCH-01-系统上下文与信任边界图")

gen_arch_01()

# ==========================================
# 2. ARCH-02 逻辑架构图
# ==========================================
def gen_arch_02():
    fig, ax = plt.subplots(figsize=(15, 11))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    
    draw_header(ax, "图 ARCH-02 逻辑架构图", 
                "Logical Architecture Diagram (UML 逻辑分层与包依赖视图)", 
                status="Proposed", 
                meta_info="关联决策: D-03, D-11, D-12, D-22, D-28, D-35 | 质量场景: QA-33, QA-40 | ADR-002, ADR-028")

    layers = [
        ("第 1 层：表现与交互层 (Presentation & Interaction Layer)", "#E1F5FE", "#0288D1", 0.79, 0.13, [
            ("Android Compose 表现子系统", "ComposeReaderScreen / SAFHandler / ConflictDiffView / 逐文件同意弹窗", 0.05, 0.42),
            ("Windows 桌面表现子系统", "WinDesktopReaderWindow / FileAccessHandler / WinConflictView / 逐文件同意弹窗", 0.53, 0.42)
        ]),
        ("第 2 层：应用编排层 (Application Orchestration Layer)", "#F1F8E9", "#558B2F", 0.63, 0.13, [
            ("客户端编排组件 (Client Coordinators)", "DocumentImportCoordinator / LocalRetrievalCoordinator / RAGInteractionCoordinator / SyncQueueCoordinator / ExportCoordinator", 0.05, 0.42),
            ("服务端用例组件 (Server UseCases)", "AccountSessionUseCase / SyncTransactionUseCase / EvidenceValidationUseCase / TaskSchedulerUseCase", 0.53, 0.42)
        ]),
        ("第 3 层：领域与公共合同层 (Domain & Contract Layer)", "#FFF8E1", "#F57F17", 0.41, 0.19, [
            ("KMP 共享核心领域候选 (Candidate Shared Core)", "• 核心实体: DocumentIdentity, DocumentVersion, SourceHash\n• 引用双锚点: SectionPath(结构) + TextRange/NormalizedRect(字符)\n• 同步核心: 不可变 SyncOperation, SyncState, IConflictPolicy, 游标合同\n• 抽象端口: IDocumentParserPort, ILocalStoragePort, ISecureStorePort", 0.05, 0.48),
            ("服务端领域模型与公共契约", "• 账号与会话: AccountEntity, DeviceSession\n• 变更流: AccountChangeLogEntry, Tombstone\n• 异步任务: AsyncTaskRecord (lease机制)\n• 契约 DTO: EvidencePackageDTO, ErrorEnvelope", 0.56, 0.39)
        ]),
        ("第 4 层：平台适配与驱动层 (Platform Adaptation Layer)", "#EDE7F6", "#512DA8", 0.23, 0.15, [
            ("Android 专有适配器", "PdfRendererAdapter / PaddleOCR / KeystoreAdapter / WorkManagerAdapter / RoomDBAdapter", 0.05, 0.28),
            ("Windows 专有适配器", "Win.Data.PdfAdapter / WinOCR / CredentialManagerAdapter / WinTaskHost / SqliteAdapter", 0.36, 0.28),
            ("服务端与外部驱动适配器", "PostgresSyncRepo / PostgresTaskRepo / OpenAIModelAdapter (Responses API) / IngressController", 0.67, 0.28)
        ]),
        ("第 5 层：基础设施与平台层 (Infrastructure Layer)", "#ECEFF1", "#455A64", 0.09, 0.11, [
            ("客户端本地基础设施", "沙箱私有文件系统 / SQLite 驱动 / ONNX 本地向量模型", 0.05, 0.42),
            ("服务端与云基础设施", "Linux OS (ECS/CCE) / RDS for PostgreSQL / LTS 日志 / KMS 密钥", 0.53, 0.42)
        ])
    ]

    for title, bg, border, y_pos, height, packages in layers:
        layer_box = patches.FancyBboxPatch((0.03, y_pos), 0.94, height, boxstyle="round,pad=0.008",
                                         facecolor=bg, edgecolor=border, lw=1.8)
        ax.add_patch(layer_box)
        ax.text(0.045, y_pos + height - 0.022, title, fontsize=9.5, fontweight='bold', color=border)
        
        for pkg_title, pkg_desc, px, pw in packages:
            pkg_box = patches.FancyBboxPatch((px, y_pos + 0.015), pw, height - 0.045, boxstyle="round,pad=0.006",
                                            facecolor='#FFFFFF', edgecolor='#B0BEC5', lw=1.2)
            ax.add_patch(pkg_box)
            ax.text(px + 0.01, y_pos + height - 0.048, pkg_title, fontsize=8.5, fontweight='bold', color='#263238')
            ax.text(px + 0.01, y_pos + 0.025, pkg_desc, fontsize=7.5, va='bottom', color='#37474F')

    # Draw Inter-layer flow arrow
    ax.annotate("", xy=(0.5, 0.06), xytext=(0.5, 0.08), arrowprops=dict(arrowstyle="->", lw=1.5, color='#455A64'))
    
    # Bottom Note
    note_box = patches.FancyBboxPatch((0.03, 0.01), 0.94, 0.065, boxstyle="round,pad=0.005",
                                     facecolor='#FFF9C4', edgecolor='#FBC02D', lw=1.2)
    ax.add_patch(note_box)
    ax.text(0.5, 0.042, "KMP 共享核心纯度与依赖倒置准则 (ADR-002, ADR-028):\n1. 共享核心只包含稳定领域模型、同步算法与无副作用测试向量；严禁引用 Android/Windows 原生 UI、文件、SQLite 或线程库。\n2. 平台层通过实现领域暴露的 Ports 注入具体能力；逻辑单元、进程与物理部署解耦，一期三个服务端单元允许同机部署。", 
            fontsize=8, ha='center', va='center', color='#F57F17')

    save_fig(fig, "ARCH-02-逻辑架构图")

gen_arch_02()

# ==========================================
# 3. ARCH-03 进程与通信边界图
# ==========================================
def gen_arch_03():
    fig, ax = plt.subplots(figsize=(15, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    
    draw_header(ax, "图 ARCH-03 进程与通信边界图", 
                "Process & Communication Boundary Diagram (UML 进程并发与通信构件视图)", 
                status="Proposed", 
                meta_info="关联决策: D-11, D-21, D-26, D-27, D-30 | 质量场景: QA-31, QA-32, QA-35 | ADR-026, ADR-027, ADR-030")

    # Android Process
    and_p = patches.FancyBboxPatch((0.03, 0.48), 0.28, 0.42, boxstyle="round,pad=0.01", facecolor='#F1F8E9', edgecolor='#558B2F', lw=2)
    ax.add_patch(and_p)
    ax.text(0.17, 0.87, "Android 客户端进程\ncom.reader.android (独立沙箱)", fontsize=9.5, fontweight='bold', ha='center', color='#33691E')
    ax.text(0.17, 0.66, "• Main UI 线程 (Compose 渲染)\n• 协程池/IO 线程 (PDF/OCR 解析)\n• WorkManager 持久任务线程\n• SQLite 本地库 (WAL 事务)\n• Binder IPC 访问系统 Keystore", fontsize=8, ha='center', va='center', color='#1B5E20')

    # Windows Process
    win_p = patches.FancyBboxPatch((0.03, 0.08), 0.28, 0.38, boxstyle="round,pad=0.01", facecolor='#E1F5FE', edgecolor='#0288D1', lw=2)
    ax.add_patch(win_p)
    ax.text(0.17, 0.42, "Windows 客户端进程\nIntelligentReader.exe (桌面进程)", fontsize=9.5, fontweight='bold', ha='center', color='#01579B')
    ax.text(0.17, 0.26, "• UI 线程 (桌面消息循环)\n• ThreadPool 工作线程 (解析/建索引)\n• TaskHost 持久任务宿主\n• SQLite 本地库 (WAL 事务)\n• Win32 DPAPI / Credential Manager", fontsize=8, ha='center', va='center', color='#0277BD')

    # Ingress / Server Box
    srv_node = patches.FancyBboxPatch((0.44, 0.15), 0.36, 0.75, boxstyle="round,pad=0.01", facecolor='#EDE7F6', edgecolor='#512DA8', lw=2)
    ax.add_patch(srv_node)
    ax.text(0.62, 0.865, "服务端计算节点 (可同机部署多个独立进程)", fontsize=10.5, fontweight='bold', ha='center', color='#311B92')
    
    # Sub-processes
    p_ing = patches.FancyBboxPatch((0.46, 0.72), 0.32, 0.11, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.2)
    ax.add_patch(p_ing)
    ax.text(0.62, 0.775, "进程 1: Ingress Proxy (TLS 1.3 终结, 路由分发, 限流)", fontsize=8.5, fontweight='bold', ha='center', color='#4527A0')

    p_sync = patches.FancyBboxPatch((0.46, 0.56), 0.15, 0.14, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.2)
    ax.add_patch(p_sync)
    ax.text(0.535, 0.63, "进程 2: sync-service\n(身份与同步单元)\nREST push/pull\n游标变更流", fontsize=8, ha='center', va='center', color='#311B92')

    p_ai = patches.FancyBboxPatch((0.63, 0.56), 0.15, 0.14, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.2)
    ax.add_patch(p_ai)
    ax.text(0.705, 0.63, "进程 3: rag-gateway\n(AI/RAG 网关单元)\n最少证据校验\nSSE 流式与轮询", fontsize=8, ha='center', va='center', color='#311B92')

    p_worker = patches.FancyBboxPatch((0.46, 0.38), 0.32, 0.14, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.2)
    ax.add_patch(p_worker)
    ax.text(0.62, 0.45, "进程 4: task-worker (后台任务调度器)\n基于 PostgreSQL 任务表 lease 抢占，自动重试与故障恢复", fontsize=8, ha='center', va='center', color='#311B92')

    p_pg = patches.FancyBboxPatch((0.46, 0.18), 0.32, 0.16, boxstyle="round,pad=0.008", facecolor='#D1C4E9', edgecolor='#311B92', lw=1.5)
    ax.add_patch(p_pg)
    ax.text(0.62, 0.26, "进程 5: postgresql (PostgreSQL 16 数据库集群)\n• 账号表 & 同步当前态表\n• AccountChangeLog 变更流表 (全局递增 revision)\n• AsyncTask 任务表 (leaseOwner, leaseUntil, idempotencyKey)", fontsize=8, ha='center', va='center', color='#1A237E')

    # External LLM Process
    ext_node = patches.FancyBboxPatch((0.85, 0.45), 0.12, 0.32, boxstyle="round,pad=0.01", facecolor='#FFF3E0', edgecolor='#E65100', lw=2)
    ax.add_patch(ext_node)
    ax.text(0.91, 0.72, "进程 6: 外部受控模型", fontsize=9, fontweight='bold', ha='center', color='#BF360C')
    ax.text(0.91, 0.58, "api.openai.com\n(Responses API 候选)\n\n• gpt-5.4-mini 快照\n• 仅单次最少证据\n• 结构化输出", fontsize=8, ha='center', va='center', color='#D84315')

    # Connectors
    # Android -> Ingress
    ax.annotate("", xy=(0.46, 0.78), xytext=(0.31, 0.72), arrowprops=dict(arrowstyle="->", lw=1.5, color='#33691E'))
    ax.text(0.38, 0.77, "HTTPS REST/SSE", fontsize=7.5, ha='center', color='#33691E')
    
    # Windows -> Ingress
    ax.annotate("", xy=(0.46, 0.74), xytext=(0.31, 0.35), arrowprops=dict(arrowstyle="->", lw=1.5, color='#01579B'))
    ax.text(0.37, 0.52, "HTTPS REST/SSE", fontsize=7.5, ha='center', color='#01579B')

    # Ingress -> Sync / AI
    ax.annotate("", xy=(0.535, 0.7), xytext=(0.535, 0.72), arrowprops=dict(arrowstyle="->", lw=1.2, color='#512DA8'))
    ax.annotate("", xy=(0.705, 0.7), xytext=(0.705, 0.72), arrowprops=dict(arrowstyle="->", lw=1.2, color='#512DA8'))

    # Sync / AI / Worker -> DB
    ax.annotate("", xy=(0.535, 0.34), xytext=(0.535, 0.56), arrowprops=dict(arrowstyle="->", lw=1.2, color='#311B92'))
    ax.annotate("", xy=(0.705, 0.34), xytext=(0.705, 0.56), arrowprops=dict(arrowstyle="->", lw=1.2, color='#311B92'))
    ax.annotate("", xy=(0.62, 0.34), xytext=(0.62, 0.38), arrowprops=dict(arrowstyle="->", lw=1.2, color='#311B92'))
    ax.text(0.62, 0.355, "TCP 5432 (单事务/Lease抢占)", fontsize=7.5, ha='center', color='#311B92')

    # AI -> Ext
    ax.annotate("", xy=(0.85, 0.63), xytext=(0.78, 0.63), arrowprops=dict(arrowstyle="->", lw=1.5, color='#E65100'))
    ax.text(0.815, 0.65, "HTTPS", fontsize=7.5, ha='center', color='#E65100')

    # Note
    note_box = patches.FancyBboxPatch((0.03, 0.01), 0.94, 0.05, boxstyle="round,pad=0.005", facecolor='#FFEBEE', edgecolor='#C62828', lw=1.2)
    ax.add_patch(note_box)
    ax.text(0.5, 0.035, "错误隔离与通信韧性机制 (D-21, D-26, D-27):\n• 同步失效：游标过期返回 REBASE_REQUIRED，客户端保留未确认本地队列；服务端单事务保证原子性。\n• AI 异常：SSE 断流后禁止拼接不确定片段，立即轮询最终结果；Worker 基于 lease 抢占与有限重试避免卡死。", fontsize=8, ha='center', va='center', color='#B71C1C')

    save_fig(fig, "ARCH-03-进程与通信边界图")

gen_arch_03()

# ==========================================
# 4. ARCH-04a 候选部署图·云中立基线
# ==========================================
def gen_arch_04a():
    fig, ax = plt.subplots(figsize=(15, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    
    draw_header(ax, "图 ARCH-04a 候选部署图·云中立基线", 
                "Candidate Deployment Diagram: Cloud-Neutral Baseline (UML 部署视图 / 状态：Proposed)", 
                status="Proposed", 
                meta_info="关联决策: D-08, D-11, D-14, D-35 | 质量场景: QA-39, QA-40 | ADR-014, ADR-035")

    # Devices
    d_and = patches.FancyBboxPatch((0.04, 0.55), 0.25, 0.32, boxstyle="round,pad=0.01", facecolor='#F1F8E9', edgecolor='#558B2F', lw=2)
    ax.add_patch(d_and)
    ax.text(0.165, 0.83, "<<device>> Android 12+ 设备", fontsize=9.5, fontweight='bold', ha='center', color='#33691E')
    ax.text(0.165, 0.70, "artifact: IntelligentReader.apk\n(签名 release 侧载安装包)\n\n• 私有沙箱文件系统\n• SQLite 本地数据库\n• 本地 BM25 + 向量索引资产", fontsize=8.5, ha='center', va='center', color='#1B5E20')

    d_win = patches.FancyBboxPatch((0.04, 0.15), 0.25, 0.32, boxstyle="round,pad=0.01", facecolor='#E1F5FE', edgecolor='#0288D1', lw=2)
    ax.add_patch(d_win)
    ax.text(0.165, 0.43, "<<device>> Windows 11 PC 电脑", fontsize=9.5, fontweight='bold', ha='center', color='#01579B')
    ax.text(0.165, 0.30, "artifact: IntelligentReader-win-x64\n(绿色解压演示包 / 安装包)\n\n• AppData/Local 本地目录\n• SQLite 本地数据库\n• 本地 BM25 + 向量索引资产", fontsize=8.5, ha='center', va='center', color='#0277BD')

    # Server Node
    srv = patches.FancyBboxPatch((0.4, 0.1), 0.42, 0.8, boxstyle="round,pad=0.01", facecolor='#EDE7F6', edgecolor='#512DA8', lw=2)
    ax.add_patch(srv)
    ax.text(0.61, 0.865, "<<node>> 标准 Linux 计算主机 (Ubuntu 22.04 LTS / 容器宿主)", fontsize=10.5, fontweight='bold', ha='center', color='#311B92')
    ax.text(0.61, 0.835, "【课程一期单机最小验证基线，满足 D-35 条件同机部署】", fontsize=8.5, ha='center', color='#5E35B1')

    # Inside Server: Ingress
    ing_box = patches.FancyBboxPatch((0.42, 0.7), 0.38, 0.11, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.2)
    ax.add_patch(ing_box)
    ax.text(0.61, 0.775, "<<executionEnvironment>> 反向代理网关 (Caddy / Nginx)", fontsize=9, fontweight='bold', ha='center', color='#4527A0')
    ax.text(0.61, 0.73, "TCP 443 监听 / TLS 1.3 证书终结 / 请求限流与路由", fontsize=8, ha='center', color='#311B92')

    # Inside Server: Apps
    app_box = patches.FancyBboxPatch((0.42, 0.45), 0.38, 0.22, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#7E57C2', lw=1.2)
    ax.add_patch(app_box)
    ax.text(0.61, 0.635, "<<executionEnvironment>> Java 21 / 容器运行时", fontsize=9, fontweight='bold', ha='center', color='#4527A0')
    ax.text(0.61, 0.54, "• artifact: sync-service.jar (8081 / HTTP 本地监听)\n• artifact: rag-gateway.jar (8082 / HTTP 本地监听)\n• artifact: task-worker.jar (后台调度器实例)", fontsize=8, ha='center', va='center', color='#311B92')

    # Inside Server: DB
    db_box = patches.FancyBboxPatch((0.42, 0.15), 0.38, 0.26, boxstyle="round,pad=0.008", facecolor='#D1C4E9', edgecolor='#311B92', lw=1.5)
    ax.add_patch(db_box)
    ax.text(0.61, 0.365, "<<database system>> PostgreSQL 16 数据库集群", fontsize=9, fontweight='bold', ha='center', color='#311B92')
    ax.text(0.61, 0.25, "artifact: intelligent_reader_db (TCP 5432)\n• 同步当前态与幂等表\n• AccountChangeLog 游标变更流\n• 异步任务表 (lease 状态与有限重试)", fontsize=8, ha='center', va='center', color='#1A237E')

    # OpenAI Cloud
    llm = patches.FancyBboxPatch((0.86, 0.45), 0.11, 0.3, boxstyle="round,pad=0.01", facecolor='#FFF3E0', edgecolor='#E65100', lw=2)
    ax.add_patch(llm)
    ax.text(0.915, 0.71, "<<node>>\nOpenAI 云基础设施", fontsize=8.5, fontweight='bold', ha='center', color='#BF360C')
    ax.text(0.915, 0.56, "Responses API\n\ngpt-5.4-mini\n(待验证快照候选)", fontsize=8, ha='center', va='center', color='#E65100')

    # Arrows
    ax.annotate("", xy=(0.42, 0.76), xytext=(0.29, 0.71), arrowprops=dict(arrowstyle="->", lw=1.8, color='#33691E'))
    ax.text(0.35, 0.75, "HTTPS 443", fontsize=8, ha='center', color='#33691E')

    ax.annotate("", xy=(0.42, 0.73), xytext=(0.29, 0.31), arrowprops=dict(arrowstyle="->", lw=1.8, color='#01579B'))
    ax.text(0.35, 0.53, "HTTPS 443", fontsize=8, ha='center', color='#01579B')

    ax.annotate("", xy=(0.86, 0.56), xytext=(0.80, 0.56), arrowprops=dict(arrowstyle="->", lw=1.8, color='#E65100'))
    ax.text(0.83, 0.59, "HTTPS", fontsize=8, ha='center', color='#E65100')

    # Note
    note_box = patches.FancyBboxPatch((0.04, 0.02), 0.93, 0.065, boxstyle="round,pad=0.005", facecolor='#FFF9C4', edgecolor='#FBC02D', lw=1.2)
    ax.add_patch(note_box)
    ax.text(0.5, 0.052, "部署视图说明与演进门槛 (ADR-014, ADR-035):\n1. 当前为 **Proposed 候选部署图**；三个逻辑服务同机部署，以控制课程验证与运维复杂度。\n2. 演进条件：仅当压力测试证明同机资源争用成为瓶颈，或安全隔离有强制要求时，才评估拆分为独立计算节点。", 
            fontsize=8, ha='center', va='center', color='#F57F17')

    save_fig(fig, "ARCH-04a-候选部署图-云中立基线")

gen_arch_04a()

# ==========================================
# 5. ARCH-04b 候选部署图·华为云能力映射
# ==========================================
def gen_arch_04b():
    fig, ax = plt.subplots(figsize=(15, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    
    draw_header(ax, "图 ARCH-04b 候选部署图·华为云能力映射", 
                "Candidate Deployment Diagram: Huawei Cloud Mapping (UML 部署视图 / 状态：Proposed)", 
                status="Proposed", 
                meta_info="关联决策: D-14, D-35 | 质量场景: QA-39, QA-40 | ADR-014, ADR-035")

    # Devices
    d_and = patches.FancyBboxPatch((0.04, 0.35), 0.22, 0.45, boxstyle="round,pad=0.01", facecolor='#F1F8E9', edgecolor='#558B2F', lw=2)
    ax.add_patch(d_and)
    ax.text(0.15, 0.75, "<<device>> 用户终端", fontsize=9.5, fontweight='bold', ha='center', color='#33691E')
    ax.text(0.15, 0.55, "Android 12+ APK\nWindows 11 Release 包\n\n• 完全端侧文档解析与渲染\n• 完全端侧混合检索与索引\n• 仅同步最小元数据与笔记", fontsize=8.5, ha='center', va='center', color='#1B5E20')

    # Huawei Cloud
    hw = patches.FancyBboxPatch((0.33, 0.1), 0.5, 0.8, boxstyle="round,pad=0.01", facecolor='#EDE7F6', edgecolor='#D32F2F', lw=2)
    ax.add_patch(hw)
    ax.text(0.58, 0.865, "华为云产品能力映射边界 (Huawei Cloud Infrastructure)", fontsize=10.5, fontweight='bold', ha='center', color='#B71C1C')
    ax.text(0.58, 0.835, "【仅为云中立架构能力的商业产品映射，业务代码无厂商 SDK 锁定】", fontsize=8.5, ha='center', color='#C62828')

    # HW ECS
    hw_ecs = patches.FancyBboxPatch((0.35, 0.52), 0.46, 0.29, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#E53935', lw=1.2)
    ax.add_patch(hw_ecs)
    ax.text(0.58, 0.775, "弹性计算能力映射: 华为云 ECS 弹性云服务器 / CCE 云容器引擎", fontsize=9, fontweight='bold', ha='center', color='#B71C1C')
    ax.text(0.58, 0.64, "• Ingress 容器 (TLS 终结、请求限流与路由分发)\n• sync-service 实例 (身份、设备会话与同步状态机)\n• rag-gateway 实例 (证据合同校验与 ModelAdapter 隔离适配)\n• task-worker 实例 (后台异步任务抢占调度)", fontsize=8, ha='center', va='center', color='#263238')

    # HW RDS
    hw_rds = patches.FancyBboxPatch((0.35, 0.15), 0.22, 0.33, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#E53935', lw=1.2)
    ax.add_patch(hw_rds)
    ax.text(0.46, 0.44, "托管数据库映射:\n华为云 RDS for PostgreSQL", fontsize=8.5, fontweight='bold', ha='center', color='#B71C1C')
    ax.text(0.46, 0.28, "• PostgreSQL 16 高可用主备\n• 自动全量/增量快照备份\n• 承载同步事务与任务表\n• 单事务保证数据一致性", fontsize=7.5, ha='center', va='center', color='#263238')

    # HW Support (LTS/DEW)
    hw_sup = patches.FancyBboxPatch((0.59, 0.15), 0.22, 0.33, boxstyle="round,pad=0.008", facecolor='#FFFFFF', edgecolor='#E53935', lw=1.2)
    ax.add_patch(hw_sup)
    ax.text(0.70, 0.44, "安全与监控映射:\n华为云 LTS / AOM / DEW", fontsize=8.5, fontweight='bold', ha='center', color='#B71C1C')
    ax.text(0.70, 0.28, "• DEW/KMS: 统一密钥与凭据注入\n• LTS: 内容无关结构化日志收集\n• AOM: APM 性能监控与告警大屏\n• 严禁记录文件正文与敏感路径", fontsize=7.5, ha='center', va='center', color='#263238')

    # Ext Model
    ext_node = patches.FancyBboxPatch((0.86, 0.45), 0.11, 0.3, boxstyle="round,pad=0.01", facecolor='#FFF3E0', edgecolor='#E65100', lw=2)
    ax.add_patch(ext_node)
    ax.text(0.915, 0.71, "<<node>>\nOpenAI 端点", fontsize=8.5, fontweight='bold', ha='center', color='#BF360C')
    ax.text(0.915, 0.56, "Responses API\n\n仅接收本次授权\n最少证据包", fontsize=8, ha='center', va='center', color='#E65100')

    # Arrows
    ax.annotate("", xy=(0.35, 0.65), xytext=(0.26, 0.58), arrowprops=dict(arrowstyle="->", lw=1.8, color='#33691E'))
    ax.text(0.30, 0.63, "HTTPS 443", fontsize=8, ha='center', color='#33691E')

    ax.annotate("", xy=(0.86, 0.65), xytext=(0.81, 0.65), arrowprops=dict(arrowstyle="->", lw=1.8, color='#E65100'))
    ax.text(0.835, 0.68, "HTTPS", fontsize=8, ha='center', color='#E65100')

    # Note
    note_box = patches.FancyBboxPatch((0.04, 0.02), 0.93, 0.065, boxstyle="round,pad=0.005", facecolor='#FFEBEE', edgecolor='#D32F2F', lw=1.2)
    ax.add_patch(note_box)
    ax.text(0.5, 0.052, "华为云能力映射约束准则 (ADR-014, 36设计说明书第23节):\n1. 本图仅作商业基础设施能力对照，所有接口采用标准 HTTPS、JDBC/TCP 等开放协议，严禁泄漏云厂商 SDK 入共享核心。\n2. RDS for PostgreSQL 提供企业级容灾备份，满足 D-30 的单事务原子性；LTS/AOM 严格遵循内容无关白名单审计。", 
            fontsize=8, ha='center', va='center', color='#B71C1C')

    save_fig(fig, "ARCH-04b-候选部署图-华为云能力映射")

gen_arch_04b()

# ==========================================
# 6. FLOW-01 至 FLOW-08 关键运行场景图
# ==========================================

# FLOW-01
def gen_flow_01():
    participants = [
        ("Android 个人用户", "Personal User", "#E1F5FE"),
        ("ComposeReaderUI", "交互界面", "#E1F5FE"),
        ("AndroidSAFHandler", "平台文件框架", "#F1F8E9"),
        ("DocumentCoordinator", "导入协调器", "#FFF8E1"),
        ("AndroidDocParser", "PDF解析适配器", "#EDE7F6"),
        ("LocalHybridIndexer", "本地混合索引", "#EDE7F6"),
        ("LocalRoomDB", "本地 SQLite 库", "#DCEDC8"),
        ("WorkManager", "后台持久任务", "#F1F8E9")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="点击“导入文档”并从系统文件树中选取 PDF"),
        dict(step=2, frm=1, to=2, text="openDocumentTree / ACTION_OPEN_DOCUMENT"),
        dict(step=3, frm=2, to=0, text="弹出系统权限对话框 (SAF 框架)"),
        dict(step=4, frm=0, to=2, text="用户确认授权读取此文档"),
        dict(step=5, frm=2, to=2, text="takePersistableUriPermission(contentUri) [持久授权]"),
        dict(step=6, frm=2, to=1, text="返回持久化 contentUri", is_ret=True),
        dict(step=7, frm=1, to=3, text="importDocument(contentUri)"),
        dict(step=8, frm=3, to=3, text="计算文件 SHA-256 哈希值 (SourceHash)"),
        dict(step=9, frm=3, to=6, text="queryDocumentByIdentity(SourceHash)"),
        dict(step=10, frm=6, to=3, text="返回 docId / null", is_ret=True),
        dict(step=11, frm=3, to=6, text="插入文档元数据表 (docId, SourceHash, contentUri)"),
        dict(step=12, frm=3, to=7, text="enqueueParsingTask(docId, versionId)"),
        dict(step=13, frm=7, to=1, text="通知任务已入队，展示后台解析进度条", is_ret=True),
        dict(step=14, frm=7, to=4, text="后台工作线程触发 parseDocument(contentUri)", alt_label="异步后台处理 (WorkManager 工作线程)"),
        dict(step=15, frm=4, to=4, text="抽取原生文本流、段落大纲与双精度 BBox 坐标"),
        dict(step=16, frm=4, to=4, text="扫描/低置信度页面触发本地 OCR (明确标注 OCR 来源)"),
        dict(step=17, frm=4, to=7, text="输出统一文档模型 DocumentPage(pages, blocks, bboxes)", is_ret=True),
        dict(step=18, frm=7, to=5, text="buildHybridIndex(docId, versionId, pages)"),
        dict(step=19, frm=5, to=5, text="构建本地 BM25 倒排索引 + 本地中文 Embedding 向量"),
        dict(step=20, frm=5, to=6, text="事务写入混合索引、文本范围映射表与初始双锚点"),
        dict(step=21, frm=5, to=7, text="混合索引构建完成", is_ret=True),
        dict(step=22, frm=7, to=1, text="StateFlow 通知解析就绪，高亮与选区功能可用", is_ret=True),
        dict(step=23, frm=1, to=0, text="界面提示“可搜索、可问答”，打开阅读器窗口", is_ret=True)
    ]
    notes = [
        dict(text="负向安全约束 (D-05, D-17, D-20): 源文档、页面位图、OCR 结果与混合索引 100% 留存在 Android 私有沙箱，绝不上云；离线完全可用。")
    ]
    draw_sequence_diagram("FLOW-01-Android文档选择授权解析与本地索引", 
                          "图 FLOW-01 时序图·Android文档选择、授权、解析与本地混合索引", 
                          "UML 时序图 / 关键场景 1", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC01, UC02 | 决策: D-05, D-20, D-22, D-23, D-26 | 质量场景: QA-27, QA-28 | ADR-022, ADR-023")

gen_flow_01()

# FLOW-02
def gen_flow_02():
    participants = [
        ("Windows 个人用户", "Personal User", "#E1F5FE"),
        ("WinDesktopReaderUI", "桌面交互界面", "#E1F5FE"),
        ("WinFileHandler", "系统文件对话框", "#F1F8E9"),
        ("WinImportCoordinator", "导入协调器", "#FFF8E1"),
        ("WinDocParser", "PDF解析适配器", "#EDE7F6"),
        ("LocalHybridIndexer", "本地混合索引", "#EDE7F6"),
        ("WinLocalSqliteStore", "AppData 本地库", "#DCEDC8"),
        ("WinTaskHost", "桌面任务宿主", "#F1F8E9")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="拖拽 PDF 文档入窗口或点击“打开本地文件”"),
        dict(step=2, frm=1, to=2, text="FileOpenPicker / DragDropEvent"),
        dict(step=3, frm=2, to=2, text="校验 Windows 文件 ACL 权限与绝对路径"),
        dict(step=4, frm=2, to=1, text="返回绝对文件路径 FilePath 与文件句柄", is_ret=True),
        dict(step=5, frm=1, to=3, text="importWindowsDocument(FilePath)"),
        dict(step=6, frm=3, to=3, text="读取文件流，计算 SHA-256 内容哈希 (SourceHash)"),
        dict(step=7, frm=3, to=6, text="queryDocumentByHash(SourceHash)"),
        dict(step=8, frm=6, to=3, text="返回现有 docId / null", is_ret=True),
        dict(step=9, frm=3, to=6, text="写入文档元数据表 (docId, SourceHash, FilePath)"),
        dict(step=10, frm=3, to=7, text="submitAsyncTask(docId, versionId, PARSE_INDEX)"),
        dict(step=11, frm=7, to=1, text="任务已排队持久化，桌面状态栏呈现进度", is_ret=True),
        dict(step=12, frm=7, to=4, text="ThreadPool 线程触发 parseDocument(FilePath)", alt_label="异步多线程处理 (TaskHost 线程池)"),
        dict(step=13, frm=4, to=4, text="Windows.Data.Pdf 抽取原生文本流与 NormalizedRect 坐标"),
        dict(step=14, frm=4, to=4, text="扫描页面触发 WinOCR 适配器抽取图像文字"),
        dict(step=15, frm=4, to=7, text="封装统一模型 DocumentPage(pages, blocks, bboxes)", is_ret=True),
        dict(step=16, frm=7, to=5, text="buildLocalHybridIndex(docId, versionId, pages)"),
        dict(step=17, frm=5, to=5, text="构建本地 BM25 全文倒排索引 + ONNX 中文向量模型"),
        dict(step=18, frm=5, to=6, text="WAL 事务写入混合索引与双锚点映射表"),
        dict(step=19, frm=5, to=7, text="混合索引构建完成", is_ret=True),
        dict(step=20, frm=7, to=1, text="PostMessage / 事件通知解析就绪", is_ret=True),
        dict(step=21, frm=1, to=0, text="目录树展开，高亮渲染完成，支持选区与即时问答", is_ret=True)
    ]
    notes = [
        dict(text="跨端一致性保证 (D-02, D-05, D-22): 尽管 Windows 原生 PDF 组件与 Android 不同，但统一输出 DocumentPage 契约，双端共享相同引用语义。")
    ]
    draw_sequence_diagram("FLOW-02-Windows文档选择访问解析与本地索引", 
                          "图 FLOW-02 时序图·Windows文档选择、路径访问、解析与本地混合索引", 
                          "UML 时序图 / 关键场景 2", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC01, UC02 | 决策: D-02, D-05, D-20, D-22, D-23, D-26 | 质量场景: QA-27, QA-28 | ADR-022, ADR-023")

gen_flow_02()

# FLOW-03
def gen_flow_03():
    participants = [
        ("个人用户", "Personal User", "#E1F5FE"),
        ("ReaderUI", "阅读与问答界面", "#E1F5FE"),
        ("RetrievalCoord", "检索协调器", "#FFF8E1"),
        ("LocalIndexer", "本地混合检索引擎", "#EDE7F6"),
        ("EvidenceTrimmer", "最少证据裁剪器", "#DCEDC8"),
        ("AIRAGGateway", "服务端 AI 网关", "#EDE7F6"),
        ("ModelAdapter", "供应商隔离适配器", "#FFF3E0"),
        ("OpenAI Cloud", "Responses API", "#FFE0B2"),
        ("CitationVerifier", "引用核验与锚点绑定", "#DCEDC8")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="选区或输入问答：“总结第三章关于架构约束的要点”"),
        dict(step=2, frm=1, to=2, text="initiateQuestion(queryText, activeDocIds)"),
        dict(step=3, frm=2, to=4, text="checkUserConsent(activeDocIds) [逐文件外发同意校验]", alt_label="阶段 1：逐文件同意与权限过滤 (D-24, QA-29)"),
        dict(step=4, frm=4, to=1, text="CONSENT_REQUIRED (若未同意，弹出隐私授权确认框)", is_ret=True),
        dict(step=5, frm=0, to=1, text="用户勾选同意并确认"),
        dict(step=6, frm=1, to=2, text="confirmConsent(activeDocIds, consentProof)"),
        dict(step=7, frm=2, to=3, text="executeHybridSearch(queryText, activeDocIds)", alt_label="阶段 2：客户端本地混合检索与裁剪 (D-23, D-24)"),
        dict(step=8, frm=3, to=3, text="本地 BM25 关键词召回 + 本地 Embedding 向量语义召回 (各Top-20)"),
        dict(step=9, frm=3, to=3, text="执行 RRF 倒数排名融合与相关性重排"),
        dict(step=10, frm=3, to=2, text="返回 Top-5 综合候选块 (含文本与 NormalizedRect)", is_ret=True),
        dict(step=11, frm=2, to=4, text="trimToMinimalEvidence(candidates, maxTokens=1500)"),
        dict(step=12, frm=4, to=4, text="裁剪无关上下文，剥离本地路径，注入 DocumentVersion 校验摘要"),
        dict(step=13, frm=4, to=2, text="生成最小证据包 EvidencePackageDTO", is_ret=True),
        dict(step=14, frm=2, to=5, text="HTTPS POST /api/v1/ai/query (EvidencePackage, QueryText)", alt_label="阶段 3：网关验证与流式生成 (D-15, D-27, D-29)"),
        dict(step=15, frm=5, to=5, text="验证证据包格式与大小合法性 (拒收全文)，记录脱敏关联 ID"),
        dict(step=16, frm=5, to=6, text="callModelStream(anonymizedEvidence, queryText)"),
        dict(step=17, frm=6, to=7, text="HTTPS POST Responses API (严格结构化输出与引用下标)"),
        dict(step=18, frm=7, to=6, text="HTTP 200 chunked SSE 增量数据流", is_ret=True),
        dict(step=19, frm=6, to=5, text="归一化流式事件 (token, citationIndex)", is_ret=True),
        dict(step=20, frm=5, to=2, text="HTTP 200 text/event-stream (SSE 流推送)", is_ret=True),
        dict(step=21, frm=2, to=1, text="打字机式平滑呈现文字回答", is_ret=True),
        dict(step=22, frm=7, to=5, text="发送 [DONE] 信号与最终结构化引用列表", is_ret=True),
        dict(step=23, frm=5, to=8, text="verifyCitations(aiCitations, EvidencePackage)", alt_label="阶段 4：客户端引用白名单核验 (D-25, QA-30)"),
        dict(step=24, frm=8, to=8, text="核验引用必在证据白名单中 (剔除幻觉引用)；绑定结构+字符双锚点"),
        dict(step=25, frm=8, to=1, text="渲染可交互的引用角标 [1]", is_ret=True),
        dict(step=26, frm=0, to=1, text="点击角标 [1] 可精准回跳原文档高亮区域")
    ]
    notes = [
        dict(text="网络断流与降级防护 (D-27): 若 SSE 意外断流，禁止拼接模糊片段，立即通过 queryId 降级为 HTTP 轮询最终结果；引用严格限定白名单。")
    ]
    draw_sequence_diagram("FLOW-03-本地混合检索最少证据选择与AI问答引用核验", 
                          "图 FLOW-03 时序图·本地混合检索、最少证据选择与AI问答引用核验", 
                          "UML 时序图 / 关键场景 3", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC03, UC04 | 决策: D-06, D-15, D-24, D-25, D-27, D-29 | 质量场景: QA-29, QA-30, QA-32, QA-34 | ADR-024, ADR-025, ADR-027, ADR-029")

gen_flow_03()

# FLOW-04
def gen_flow_04():
    participants = [
        ("个人用户", "Personal User", "#E1F5FE"),
        ("Android 客户端 A", "移动端应用", "#F1F8E9"),
        ("服务端同步单元", "sync-service", "#EDE7F6"),
        ("PostgreSQL 数据库", "同步与变更流表", "#D1C4E9"),
        ("Windows 客户端 B", "桌面端应用", "#E1F5FE")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="在文档第 45 页做高亮笔记：“分层需要严格隔离”"),
        dict(step=2, frm=1, to=1, text="本地写入 NoteEntity；创建不可变 SyncOperation (UUID-101, baseRev=12)"),
        dict(step=3, frm=1, to=1, text="标记操作为 PENDING_UPLOAD"),
        dict(step=4, frm=1, to=2, text="HTTPS POST /api/v1/sync/push (operationId, baseRev: 12, operations)", alt_label="阶段 1：客户端 A 发起原子 Push (D-21, D-30)"),
        dict(step=5, frm=2, to=2, text="验证设备会话，检查 operationId 幂等表，防重复副作用"),
        dict(step=6, frm=2, to=3, text="BEGIN TRANSACTION (开启单数据库事务)"),
        dict(step=7, frm=3, to=3, text="更新资源当前态 (UPDATE UserNotes)"),
        dict(step=8, frm=3, to=3, text="固化幂等记录 (INSERT INTO IdempotencyRecord)"),
        dict(step=9, frm=3, to=3, text="追加变更流 (INSERT INTO AccountChangeLog VALUES (Rev-13, UUID-101))"),
        dict(step=10, frm=2, to=3, text="COMMIT TRANSACTION (事务提交成功)"),
        dict(step=11, frm=2, to=1, text="HTTP 200 OK (confirmedRevision: 13, newCursor: 'cur_rev13_seq1')", is_ret=True),
        dict(step=12, frm=1, to=1, text="标记 UUID-101 为 COMMITTED，更新本地游标为 cur_rev13_seq1"),
        dict(step=13, frm=4, to=2, text="HTTPS GET /api/v1/sync/pull?cursor=cur_rev12_seq0", alt_label="阶段 2：客户端 B 主动 Pull 拉取并幂等应用 (D-21, QA-35)"),
        dict(step=14, frm=2, to=3, text="SELECT * FROM AccountChangeLog WHERE revision > 12"),
        dict(step=15, frm=3, to=2, text="返回 Rev-13 笔记变更数据与最新游标 cur_rev13_seq1", is_ret=True),
        dict(step=16, frm=2, to=4, text="HTTP 200 OK (changes: [Rev-13], nextCursor: 'cur_rev13_seq1')", is_ret=True),
        dict(step=17, frm=4, to=4, text="检查本地是否已有 operationId (幂等过滤)，应用笔记入本地库"),
        dict(step=18, frm=4, to=4, text="按文档 SHA-256 哈希自动关联本地已授权原文件"),
        dict(step=19, frm=4, to=4, text="更新本地游标为 cur_rev13_seq1"),
        dict(step=20, frm=4, to=0, text="桌面端静默同步完成，第 45 页出现对应高亮与笔记", is_ret=True)
    ]
    notes = [
        dict(text="事务与幂等保证 (D-21, D-30): 服务端在单一 PostgreSQL 事务更新当前态、固化幂等键并追加变更日志；游标失效返回 REBASE_REQUIRED 保留本地未确认操作。")
    ]
    draw_sequence_diagram("FLOW-04-双端阅读进度与笔记同步时序图", 
                          "图 FLOW-04 时序图·双端阅读进度与笔记同步时序图", 
                          "UML 时序图 / 关键场景 4", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC05 | 决策: D-04, D-17, D-21, D-30 | 质量场景: QA-31, QA-35 | ADR-017, ADR-021, ADR-030")

gen_flow_04()

# FLOW-05
def gen_flow_05():
    participants = [
        ("个人用户", "Personal User", "#E1F5FE"),
        ("Android 客户端 A", "离线编辑", "#F1F8E9"),
        ("Windows 客户端 B", "离线编辑", "#E1F5FE"),
        ("服务端同步单元", "sync-service", "#EDE7F6"),
        ("PostgreSQL 数据库", "同步状态机", "#D1C4E9")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="离线修改 Note-X：“方案 A 采用 KMP 架构” 并翻至第 50 页"),
        dict(step=2, frm=1, to=1, text="本地入队 Op-A (entityId: Note-X, baseRev: 10, page: 50)"),
        dict(step=3, frm=0, to=2, text="离线修改同一 Note-X：“方案 A 采用 Rust 架构” 并翻至第 65 页"),
        dict(step=4, frm=2, to=2, text="本地入队 Op-B (entityId: Note-X, baseRev: 10, page: 65)"),
        dict(step=5, frm=1, to=3, text="HTTPS POST /sync/push (Op-A, baseRev: 10)", alt_label="阶段 1：客户端 A 先行联网并提交成功"),
        dict(step=6, frm=3, to=4, text="校验 baseRev=10 等于当前服务端版本，更新 Note-X，生成 Rev-11"),
        dict(step=7, frm=4, to=3, text="事务提交成功", is_ret=True),
        dict(step=8, frm=3, to=1, text="HTTP 200 OK (confirmedRevision: 11)", is_ret=True),
        dict(step=9, frm=2, to=3, text="HTTPS POST /sync/push (Op-B, baseRev: 10)", alt_label="阶段 2：客户端 B 后续提交，检测到版本碰撞 (D-13 A)"),
        dict(step=10, frm=3, to=4, text="检查发现 Note-X 当前版本已是 Rev-11 (baseRev 10 已过期！)"),
        dict(step=11, frm=3, to=3, text="执行 ADR-013 策略：进度按时间戳取最新(65页)；笔记绝不覆盖，生成冲突副本！"),
        dict(step=12, frm=3, to=4, text="事务插入 NoteConflictCopy 实体 (sourceDevice='Windows-ClientB')"),
        dict(step=13, frm=4, to=4, text="追加变更流生成 Rev-12"),
        dict(step=14, frm=3, to=2, text="HTTP 200 OK (status: CONFLICT_COPY_CREATED, confirmedRev: 12)", is_ret=True),
        dict(step=15, frm=2, to=2, text="本地更新基线为 Rev-12，持久化 NoteConflictCopy"),
        dict(step=16, frm=2, to=0, text="界面弹出告警横幅：“检测到异地修改冲突，已为您保留冲突副本！”", is_ret=True),
        dict(step=17, frm=0, to=2, text="用户点击“查看冲突对比 (Diff View)”"),
        dict(step=18, frm=2, to=0, text="并排呈现：[云端当前主版本] vs [您的离线修改副本]，支持手动合并", is_ret=True)
    ]
    notes = [
        dict(text="零静默覆盖准则 (D-13): 并发修改发生时不猜测用户意图，笔记冲突副本作为一等公民存入两端与云端，通过 UI 显式由用户决策。")
    ]
    draw_sequence_diagram("FLOW-05-离线并发修改同步冲突与冲突保留处理时序图", 
                          "图 FLOW-05 时序图·离线并发修改、同步冲突与冲突保留处理时序图", 
                          "UML 时序图 / 关键场景 5", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC05 | 决策: D-13, D-21 | 质量场景: QA-35 | ADR-013, ADR-021")

gen_flow_05()

# FLOW-06
def gen_flow_06():
    participants = [
        ("个人用户", "Personal User", "#E1F5FE"),
        ("ReaderUI", "阅读与引用界面", "#E1F5FE"),
        ("DocCoordinator", "文档生命周期", "#FFF8E1"),
        ("LocalStorage", "本地版本与引用库", "#DCEDC8"),
        ("CitationEngine", "引用与 stale 判定", "#EDE7F6"),
        ("DocParserAdapter", "文档解析适配器", "#EDE7F6")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="打开本地已编辑/更新后的文档《系统设计说明书.pdf》"),
        dict(step=2, frm=1, to=2, text="openDocument(contentUri / filePath)"),
        dict(step=3, frm=2, to=2, text="读取文件流，重新计算 SHA-256 哈希值 (得到 CurrentHash)"),
        dict(step=4, frm=2, to=3, text="queryDocumentVersion(docId)"),
        dict(step=5, frm=3, to=2, text="返回既有版本信息 (VersionId: V1, RecordedHash: Hash_Old)", is_ret=True),
        dict(step=6, frm=2, to=2, text="哈希比对: CurrentHash != Hash_Old -> 触发 LIKELY_NEW_VERSION (D-16)", alt_label="阶段 1：建立新版本与引用保守失效 (D-16 A, D-25 A)"),
        dict(step=7, frm=2, to=3, text="createNewVersion(docId, newVersionId: V2, CurrentHash, parent: V1)"),
        dict(step=8, frm=2, to=4, text="evaluateCitationsOnVersionChange(docId, oldVer: V1, newVer: V2)"),
        dict(step=9, frm=4, to=3, text="获取所有绑定在 V1 上的历史引用与高亮项 (CitationRefList)"),
        dict(step=10, frm=4, to=4, text="执行保守失效规则：禁止静默迁移！将引用置为 isStale=true (STALE_CONTENT_MUTATED)"),
        dict(step=11, frm=4, to=3, text="updateCitationStatus(refId, isStale=true)"),
        dict(step=12, frm=2, to=1, text="载入文档，界面顶部弹出黄色告警横幅：“文档已变更为 V2，既有引用已标为 stale”", is_ret=True),
        dict(step=13, frm=0, to=1, text="用户点击带有 [stale] 警示图标的旧引用", alt_label="阶段 2：UI 呈现陈旧状态与手动重新对齐"),
        dict(step=14, frm=1, to=4, text="inspectStaleCitation(refId)"),
        dict(step=15, frm=4, to=1, text="返回历史快照文本、原页码及失效说明", is_ret=True),
        dict(step=16, frm=1, to=0, text="呈现历史引用摘录：“该引用来自上一版本第 32 页，无法在新版本中自动对齐”", is_ret=True),
        dict(step=17, frm=0, to=1, text="用户点击“在新版本中重新搜索并重新锚定”"),
        dict(step=18, frm=1, to=5, text="reparseDocument(contentUri, V2) 并定位最接近匹配段落"),
        dict(step=19, frm=5, to=4, text="返回新版本页面文本块与新 NormalizedRect", is_ret=True),
        dict(step=20, frm=4, to=3, text="updateAnchor(refId, newVersionId: V2, newRect, isStale=false)"),
        dict(step=21, frm=4, to=1, text="刷新界面，[stale] 标记清除，恢复正常回跳高亮", is_ret=True)
    ]
    notes = [
        dict(text="引用完整性准则 (D-25, QA-30): 跨版本默认 stale，绝不自动模糊漂移到错误文字；必须保留来源与置信度供人工核验。")
    ]
    draw_sequence_diagram("FLOW-06-文档版本变化引用失效stale与重新授权时序图", 
                          "图 FLOW-06 时序图·文档版本变化、引用失效(stale)与重新授权时序图", 
                          "UML 时序图 / 关键场景 6", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC02, UC04 | 决策: D-16, D-25 | 质量场景: QA-30 | ADR-016, ADR-025")

gen_flow_06()

# FLOW-07
def gen_flow_07():
    participants = [
        ("个人用户", "Personal User", "#E1F5FE"),
        ("客户端应用 A", "操作设备", "#F1F8E9"),
        ("BackgroundTaskRunner", "后台任务管理器", "#F1F8E9"),
        ("LocalStorage", "本地沙箱与缓存", "#DCEDC8"),
        ("服务端同步单元", "sync-service", "#EDE7F6"),
        ("PostgreSQL 数据库", "同步与任务表", "#D1C4E9"),
        ("对端客户端 B", "同步目标设备", "#E1F5FE")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="在界面上对某文档点击“撤销授权并移除”或“删除笔记”"),
        dict(step=2, frm=1, to=1, text="立即阻断该文档新的本地检索与 AI 问答请求"),
        dict(step=3, frm=1, to=2, text="cancelActiveTasksForDocument(docId) [终止运行中任务]", alt_label="阶段 1：活动任务立即停止与本地清理 (D-26, QA-29)"),
        dict(step=4, frm=2, to=2, text="Thread.interrupt / Coroutine.cancel 强制终止解析/OCR/建索引工作"),
        dict(step=5, frm=2, to=1, text="确认运行中任务已安全终止", is_ret=True),
        dict(step=6, frm=1, to=3, text="purgeLocalArtifacts(docId) [清理本地残留]"),
        dict(step=7, frm=3, to=3, text="删除页面位图缓存、删除本地 BM25/向量条目、释放 SAF URI 授权"),
        dict(step=8, frm=3, to=1, text="本地沙箱残留清理完毕", is_ret=True),
        dict(step=9, frm=1, to=4, text="HTTPS POST /sync/push (operationId, type: DELETE_DOC, isTombstone: true)", alt_label="阶段 2：生成 Tombstone 并向服务端与对端广播 (D-19, D-21)"),
        dict(step=10, frm=4, to=5, text="事务更新：isDeleted=true；在 Tombstones 表插入墓碑；追加变更流 Rev-25"),
        dict(step=11, frm=5, to=4, text="事务提交成功", is_ret=True),
        dict(step=12, frm=4, to=1, text="HTTP 200 OK (confirmedRevision: 25)", is_ret=True),
        dict(step=13, frm=6, to=4, text="HTTPS GET /sync/pull?cursor=cur_rev24"),
        dict(step=14, frm=4, to=6, text="返回 Rev-25 (Tombstone: docId 已删除)", is_ret=True),
        dict(step=15, frm=6, to=6, text="识别 Tombstone 指令：严禁静默复活！终止后台任务，下架本地该文档与笔记"),
        dict(step=16, frm=6, to=0, text="对端设备完成静默清理，无任何数据残留", is_ret=True),
        dict(step=17, frm=4, to=5, text="经过保留窗口期后，Worker 执行物理硬删除清理过期墓碑", alt_label="阶段 3：服务端异步硬删除 (D-19 A)")
    ]
    notes = [
        dict(text="防删除复活与生命周期清理 (D-19, D-26): 墓碑广播彻底阻断旧数据复活；运行中任务被强力终止，确保文件撤权后无数据泄露。")
    ]
    draw_sequence_diagram("FLOW-07-文件撤权删除传播活动任务停止与残留清理时序图", 
                          "图 FLOW-07 时序图·文件撤权、删除传播、活动任务停止与残留清理时序图", 
                          "UML 时序图 / 关键场景 7", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC05 | 决策: D-05, D-19, D-20, D-26 | 质量场景: QA-29, QA-31 | ADR-019, ADR-020, ADR-026")

gen_flow_07()

# FLOW-08
def gen_flow_08():
    participants = [
        ("个人用户", "Personal User", "#E1F5FE"),
        ("ReaderUI", "阅读与导出界面", "#E1F5FE"),
        ("ExportCoordinator", "导出协调器", "#FFF8E1"),
        ("LocalStorage", "本地笔记与版本库", "#DCEDC8"),
        ("ExportFormatter", "统一导出语义格式化", "#EDE7F6"),
        ("PlatformStorageBridge", "系统文件写入适配器", "#F1F8E9")
    ]
    steps = [
        dict(step=1, frm=0, to=1, text="点击“导出笔记与分析”，选择目标格式 (Markdown 或 PDF)"),
        dict(step=2, frm=1, to=2, text="initiateExport(docId, format: MD_OR_PDF)"),
        dict(step=3, frm=2, to=3, text="queryExportPayload(docId)"),
        dict(step=4, frm=3, to=2, text="返回 SourceHash, VersionId, 笔记列表与已核验引用列表", is_ret=True),
        dict(step=5, frm=2, to=4, text="formatExportDocument(meta, notes, citations, format)", alt_label="阶段 1：统一导出语义生成与引用溯源绑定 (D-10, D-25)"),
        dict(step=6, frm=4, to=4, text="构建统一溯源头: 文档名、不可逆哈希 SHA-256、版本 ID、导出时间戳"),
        dict(step=7, frm=4, to=4, text="编排笔记正文结构；嵌入结构锚点(章节)与字符锚点(页码/NormalizedRect)"),
        dict(step=8, frm=4, to=4, text="若某引用处于 stale 状态，明确追加注记：“[警告：该引用为陈旧版本快照]”"),
        dict(step=9, frm=4, to=2, text="生成格式化文件流 (MD 纯文本 / PDF 矢量排版数据)", is_ret=True),
        dict(step=10, frm=2, to=5, text="requestPlatformWritePermission(targetFileName, format)", alt_label="阶段 2：平台安全写入授权与文件落盘 (D-02, D-10)"),
        dict(step=11, frm=5, to=1, text="Android 触发 ACTION_CREATE_DOCUMENT / Windows 弹出 SaveFileDialog"),
        dict(step=12, frm=0, to=5, text="选取保存路径与目录并授权写入"),
        dict(step=13, frm=5, to=2, text="返回安全输出流 targetOutputStream", is_ret=True),
        dict(step=14, frm=2, to=4, text="streamWriteToTarget(targetOutputStream) [流式分块写入，防内存溢出]"),
        dict(step=15, frm=4, to=2, text="写入完成，关闭流句柄", is_ret=True),
        dict(step=16, frm=2, to=1, text="导出成功，返回输出文件绝对路径 / URI", is_ret=True),
        dict(step=17, frm=1, to=0, text="界面弹出成功通知：“导出完成！”，提供“打开文件”与“在文件夹中查看”按钮", is_ret=True)
    ]
    notes = [
        dict(text="一期导出语义与分享边界 (D-01, D-10): 仅支持用户本地主动导出文件，严禁自动生成 Web 分享链接或云端公开链接；导出核心在于保留完整版本溯源信息。")
    ]
    draw_sequence_diagram("FLOW-08-Markdown与PDF主动导出及版本引用保留时序图", 
                          "图 FLOW-08 时序图·Markdown与PDF主动导出及版本引用保留时序图", 
                          "UML 时序图 / 关键场景 8", 
                          participants, steps, notes, status="Proposed", 
                          meta_info="关联: UC06 | 决策: D-01, D-10, D-22, D-25 | 质量场景: QA-27, QA-30 | ADR-022, ADR-025")

gen_flow_08()
print("All 13 Diagrams Generated Successfully!")
