#!/usr/bin/env python3
import sys

with open('app/src/main/assets/chromaos/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add OS Control Panel Card HTML right after the OS Quick Bar
os_box_end_marker = '<!-- MAIN STUDIO NAVIGATION TABS -->'
if os_box_end_marker not in content:
    print('Error: os_box_end_marker not found')
    sys.exit(1)

control_panel_card_html = '''            <!-- OS CONTROL PANEL & NOTIFICATION PANEL CHANGER CARD -->
            <div id="osControlPanelCard" class="glass-panel rounded-3xl p-5 border border-amber-400/25 shadow-2xl relative overflow-hidden space-y-4">
                <div class="flex flex-wrap items-center justify-between gap-2 border-b border-white/10 pb-3">
                    <div class="flex items-center gap-2.5">
                        <div class="w-10 h-10 rounded-2xl flex items-center justify-center text-white font-bold bg-amber-500/20 border border-amber-400/40 shadow-lg">
                            <i class="ph-fill ph-sliders-horizontal text-xl text-amber-300"></i>
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <h3 class="text-sm font-bold uppercase tracking-wider text-white font-stylish-display">Control &amp; Notification Panel Changer</h3>
                                <span class="px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase bg-amber-400/20 text-amber-300 border border-amber-400/30">Live Hub</span>
                            </div>
                            <p class="text-[11px] text-slate-300 mt-0.5">Transforms toggles, sliders, and notification cards according to the selected OS</p>
                        </div>
                    </div>
                    <div class="flex items-center gap-2">
                        <!-- Mode Switcher: Control Center vs Notifications -->
                        <div class="flex items-center bg-black/50 p-1 rounded-2xl border border-white/10">
                            <button onclick="setControlPanelMode('control')" id="btnControlModeToggle" class="px-3 py-1.5 rounded-xl text-xs font-bold transition-all bg-amber-400 text-black shadow">
                                <i class="ph-bold ph-faders"></i> Controls
                            </button>
                            <button onclick="setControlPanelMode('notification')" id="btnNotificationModeToggle" class="px-3 py-1.5 rounded-xl text-xs font-bold transition-all text-slate-300 hover:text-white">
                                <i class="ph-bold ph-bell-simple"></i> Notifications
                            </button>
                        </div>
                        <!-- Test Full Overlay Button -->
                        <button onclick="openControlPanelOverlay()" class="px-3.5 py-2 rounded-2xl bg-amber-400/20 hover:bg-amber-400/30 text-amber-200 border border-amber-400/40 text-xs font-bold flex items-center gap-1.5 shadow-sm active:scale-95 transition-all">
                            <i class="ph-bold ph-arrows-out-simple"></i>
                            <span>Open on Phone</span>
                        </button>
                    </div>
                </div>

                <!-- OS Control Center Style Switcher Pills -->
                <div class="space-y-2">
                    <div class="flex items-center justify-between text-xs text-slate-300">
                        <span class="font-bold flex items-center gap-1.5 text-amber-300/90">
                            <i class="ph-fill ph-palette"></i> Active Style Theme:
                        </span>
                        <span id="activeControlStyleLabel" class="font-mono text-amber-300 font-bold">Android 17 Expressive</span>
                    </div>
                    <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2" id="controlPanelOsSelectorGrid">
                        <!-- Generated via JS -->
                    </div>
                </div>

                <!-- Live Embedded Interactive Preview Box -->
                <div class="space-y-2">
                    <div class="flex items-center justify-between text-[11px] text-slate-300">
                        <span class="font-semibold flex items-center gap-1 text-slate-400">
                            <i class="ph-bold ph-eye"></i> Live Interactive Mini-Preview:
                        </span>
                        <span class="text-[10px] text-slate-400">Tap toggles & sliders to test interactive behavior</span>
                    </div>
                    <div id="controlPanelMiniPreviewContainer" class="rounded-2xl bg-black/60 border border-white/15 p-4 shadow-inner max-h-[360px] overflow-y-auto">
                        <!-- Live Mini Preview Injected via JS -->
                    </div>
                </div>
            </div>

'''

content = content.replace(os_box_end_marker, control_panel_card_html + os_box_end_marker, 1)

# 2. Add Full Control Panel & Notification Drawer Modal before exportModal
export_modal_marker = '<div id="exportModal"'
if export_modal_marker not in content:
    print('Error: export_modal_marker not found')
    sys.exit(1)

control_modal_html = '''    <!-- FULL INTERACTIVE CONTROL CENTER & NOTIFICATION PANEL DRAWER (MODAL OVERLAY) -->
    <div id="controlPanelDrawerModal" class="fixed inset-0 z-50 hidden bg-black/80 backdrop-blur-md flex items-end sm:items-center justify-center p-0 sm:p-4">
        <div class="glass-panel rounded-t-[36px] sm:rounded-3xl max-w-lg w-full p-5 border border-white/20 space-y-4 max-h-[90vh] flex flex-col shadow-2xl relative overflow-hidden" id="controlPanelDrawerInner">
            <!-- Dismiss Drag Handle -->
            <div class="w-12 h-1 rounded-full bg-white/30 mx-auto -mt-1 mb-1 sm:hidden"></div>

            <!-- Drawer Header -->
            <div class="flex items-center justify-between border-b border-white/10 pb-3 shrink-0">
                <div class="flex items-center gap-2.5">
                    <div class="w-9 h-9 rounded-xl flex items-center justify-center text-white font-bold bg-amber-500/20 border border-amber-400/30 shadow">
                        <i id="drawerHeaderIcon" class="ph-fill ph-sliders-horizontal text-lg text-amber-300"></i>
                    </div>
                    <div>
                        <h4 id="drawerHeaderTitle" class="text-sm font-bold text-white font-stylish-display">Control Center</h4>
                        <span id="drawerHeaderSubtitle" class="text-[10px] text-amber-300 font-mono">Pixel 10 Material You</span>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <div class="flex items-center bg-black/50 p-0.5 rounded-xl border border-white/10 text-xs">
                        <button onclick="setControlPanelMode('control')" id="drawerToggleControlBtn" class="px-2.5 py-1 rounded-lg font-bold bg-amber-400 text-black">Control</button>
                        <button onclick="setControlPanelMode('notification')" id="drawerToggleNotifBtn" class="px-2.5 py-1 rounded-lg font-bold text-slate-300">Notifs</button>
                    </div>
                    <button onclick="closeControlPanelOverlay()" class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center">
                        <i class="ph-bold ph-x"></i>
                    </button>
                </div>
            </div>

            <!-- Drawer Body -->
            <div id="controlPanelDrawerBody" class="overflow-y-auto space-y-3 flex-1 pr-1">
                <!-- Dynamically rendered full control panel / notification panel -->
            </div>
        </div>
    </div>

'''

content = content.replace(export_modal_marker, control_modal_html + export_modal_marker, 1)

# Write updated file
with open('app/src/main/assets/chromaos/index.html', 'w', encoding='utf-8') as f:
    f.write(content)

print('Successfully added Control Panel Card and Modal HTML!')
