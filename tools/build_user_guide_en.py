import os
import base64

IMG_DIR = "documentation/screenshots/annotated"
OUT_FILE = "documentation/source/user_guide_en.html"

def get_b64(filename):
    path = os.path.join(IMG_DIR, filename)
    if not os.path.exists(path):
        print(f"Warning: {filename} not found in {IMG_DIR}")
        return ""
    with open(path, "rb") as f:
        data = base64.b64encode(f.read()).decode("utf-8")
    return f"data:image/png;base64,{data}"

def get_b64_file(path):
    if not os.path.exists(path):
        return ""
    with open(path, "rb") as f:
        data = base64.b64encode(f.read()).decode("utf-8")
    return f"data:image/png;base64,{data}"

def build():
    print("Loading screenshots for English guide...")
    app_icon = get_b64_file("composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher.png")
    imgs = {
        "01": get_b64("01_home_dashboard.png"),
        "02": get_b64("02_routines_empty.png"),
        "03": get_b64("03_create_routine.png"),
        "04": get_b64("04_routines_list.png"),
        "05": get_b64("05_exercise_catalog.png"),
        "06": get_b64("06_exercise_detail.png"),
        "06b": get_b64("06b_create_exercise.png"),
        "08": get_b64("08_active_workout_with_timer.png"),
        "09": get_b64("09_dialog_custom_weight.png"),
        "10": get_b64("10_dialog_custom_reps.png"),
        "12": get_b64("12_workout_finish_summary.png"),
        "13": get_b64("13_progress_analytics.png"),
        "14": get_b64("14_profile_dashboard.png"),
        "15": get_b64("15_measures_modal.png"),
        "16": get_b64("16_settings_main.png"),
        "17": get_b64("17_settings_notifications.png"),
        "18": get_b64("18_dialog_custom_reminder.png"),
        "19": get_b64("19_backup_management.png"),
    }
    print("Screenshots ready. Generating English HTML...")

    css = """
    @page {
        size: 210mm 297mm;
        margin: 0;
    }
    * {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
    }
    body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        background-color: #0B0F17;
        color: #F1F5F9;
        font-size: 8.6pt;
        line-height: 1.48;
        width: 210mm;
    }
    .page {
        width: 210mm;
        height: 297mm;
        max-height: 297mm;
        padding: 12mm 15mm 10mm 15mm;
        page-break-after: always;
        break-after: page;
        page-break-inside: avoid;
        break-inside: avoid;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        position: relative;
        overflow: hidden;
        background: #0B0F17;
    }
    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #1E293B;
        padding-bottom: 4px;
        margin-bottom: 8px;
        font-size: 7pt;
        text-transform: uppercase;
        letter-spacing: 0.8px;
        color: #64748B;
    }
    .header .brand {
        font-weight: 700;
        color: #38BDF8;
    }
    .footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #1E293B;
        padding-top: 4px;
        margin-top: 8px;
        font-size: 7pt;
        color: #64748B;
    }
    .content {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
        overflow: hidden;
    }
    h1 {
        font-size: 17pt;
        font-weight: 800;
        color: #F8FAFC;
        letter-spacing: -0.3px;
        margin-bottom: 4px;
    }
    h2 {
        font-size: 11.5pt;
        font-weight: 700;
        color: #F8FAFC;
        letter-spacing: -0.2px;
        margin-bottom: 4px;
    }
    h3 {
        font-size: 9pt;
        font-weight: 700;
        color: #E2E8F0;
        margin-top: 6px;
        margin-bottom: 3px;
    }
    p {
        color: #94A3B8;
        margin-bottom: 6px;
    }
    .lead {
        font-size: 9pt;
        line-height: 1.5;
        color: #CBD5E1;
        margin-bottom: 8px;
    }
    
    /* Layout Patterns */
    .grid-2col {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
    }
    .feature-layout {
        display: grid;
        grid-template-columns: 74mm 1fr;
        gap: 16px;
        align-items: start;
    }
    
    /* Screenshot Container */
    .screenshot-frame {
        background: #000;
        border: 1px solid #1E293B;
        border-radius: 9px;
        overflow: hidden;
        box-shadow: 0 4px 14px rgba(0,0,0,0.45);
        text-align: center;
    }
    .screenshot-frame img {
        width: 100%;
        max-height: 160mm;
        object-fit: contain;
        display: block;
    }
    .caption {
        font-size: 6.8pt;
        color: #64748B;
        padding: 3px;
        background: #0D131E;
        border-top: 1px solid #1E293B;
    }
    
    /* Callout & Legend styling */
    .callout-list {
        list-style: none;
        margin: 4px 0 6px 0;
    }
    .callout-item {
        display: flex;
        align-items: flex-start;
        margin-bottom: 6px;
        font-size: 8.2pt;
        line-height: 1.42;
    }
    .callout-num {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 17px;
        height: 17px;
        border-radius: 50%;
        background: #0284C7;
        color: #FFFFFF;
        font-size: 7.2pt;
        font-weight: 700;
        margin-right: 7px;
        margin-top: 1px;
        flex-shrink: 0;
    }
    .callout-title {
        color: #F1F5F9;
        font-weight: 600;
        margin-right: 4px;
    }
    .callout-desc {
        color: #94A3B8;
    }
    
    /* Step list */
    .step-list {
        margin: 4px 0 6px 14px;
        color: #94A3B8;
        font-size: 8.2pt;
    }
    .step-list li {
        margin-bottom: 3px;
        line-height: 1.42;
    }
    .step-list strong {
        color: #E2E8F0;
    }
    
    /* Notes & Tips */
    .note-box {
        border-left: 3px solid #38BDF8;
        background: #111827;
        border-radius: 0 5px 5px 0;
        padding: 6px 10px;
        margin: 8px 0;
        font-size: 8pt;
        color: #94A3B8;
        line-height: 1.42;
    }
    .note-box strong {
        color: #F1F5F9;
    }
    
    /* Minimal Tables */
    .clean-table {
        width: 100%;
        border-collapse: collapse;
        margin: 6px 0;
        font-size: 8pt;
    }
    .clean-table th {
        text-align: left;
        color: #CBD5E1;
        font-weight: 600;
        padding: 4px 6px;
        border-bottom: 1px solid #334155;
        background: #0F172A;
    }
    .clean-table td {
        padding: 4px 6px;
        color: #94A3B8;
        border-bottom: 1px solid #1E293B;
        vertical-align: top;
    }
    .clean-table tr:last-child td {
        border-bottom: none;
    }
    
    /* Cover Page */
    .cover-wrap {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        padding: 24mm 10mm 15mm 10mm;
        text-align: center;
        background: radial-gradient(circle at 50% 25%, #172033 0%, #0B0F17 75%);
        border: 1px solid #1E293B;
        border-radius: 12px;
    }
    .cover-badge {
        display: inline-block;
        padding: 4px 14px;
        background: rgba(56, 189, 248, 0.12);
        color: #38BDF8;
        border: 1px solid rgba(56, 189, 248, 0.3);
        border-radius: 20px;
        font-size: 8.5pt;
        font-weight: 600;
        letter-spacing: 1px;
        text-transform: uppercase;
        margin-bottom: 24px;
    }
    .cover-title {
        font-size: 34pt;
        font-weight: 800;
        color: #FFFFFF;
        letter-spacing: -0.5px;
        margin-bottom: 8px;
    }
    .cover-subtitle {
        font-size: 13pt;
        color: #94A3B8;
        font-weight: 400;
        max-width: 130mm;
        margin: 0 auto 30px auto;
        line-height: 1.5;
    }
    .cover-meta {
        font-size: 8.5pt;
        color: #64748B;
        line-height: 1.6;
    }
    .cover-meta strong {
        color: #CBD5E1;
    }
    
    /* Q&A layout for Troubleshooting */
    .qa-group {
        margin-bottom: 10px;
    }
    .qa-question {
        color: #F1F5F9;
        font-weight: 700;
        font-size: 8.5pt;
        margin-bottom: 2px;
    }
    .qa-answer {
        color: #94A3B8;
        font-size: 8.2pt;
        line-height: 1.42;
    }
    """

    pages = []

    # ==========================================
    # PAGE 1: COVER
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-cover">
        <div class="cover-wrap">
            <div>
                <div class="cover-badge">Official Product Documentation</div>
                <div style="margin-bottom: 14px;"><img src="{app_icon}" style="width: 76px; height: 76px; border-radius: 18px; box-shadow: 0 8px 24px rgba(0,0,0,0.6);" alt="KumaLift Icon"></div>
                <div class="cover-title">KumaLift</div>
                <div class="cover-subtitle">User Guide & Technical Reference Manual for Offline-First Strength Training Tracking</div>
            </div>
            
            <div style="max-width: 140mm; margin: 0 auto; text-align: left; background: #111827; border: 1px solid #1E293B; border-radius: 8px; padding: 14px 18px;">
                <div style="font-size: 9pt; color: #E2E8F0; font-weight: 700; margin-bottom: 6px;">About This Document</div>
                <div style="font-size: 8.2pt; color: #94A3B8; line-height: 1.5;">
                    This comprehensive user guide provides clear, practical, and systematic instructions covering every feature in KumaLift version 1.0.0. Each section is accompanied by authentic application screenshots and verified callouts to facilitate effortless navigation and interface comprehension.
                </div>
            </div>

            <div class="cover-meta">
                <div>Application Version: <strong>1.0.0 (Production Release)</strong></div>
                <div>Distribution Format: <strong>Android APK (Offline-First SQLite)</strong></div>
                <div>Publication Date: <strong>November 2026</strong></div>
                <div style="margin-top: 8px; color: #475569;">© 2026 BearBones Development. All rights reserved.</div>
            </div>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 2: TABLE OF CONTENTS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-toc">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Document Table of Contents</span>
            <span>Page 2</span>
        </div>
        <div class="content">
            <h1>Table of Contents</h1>
            <p class="lead">Navigation outline for the KumaLift User Manual (English Edition).</p>

            <table class="clean-table" style="margin-top: 4px;">
                <thead>
                    <tr>
                        <th style="width: 65px;">Chapter</th>
                        <th>Feature Topic & Discussion</th>
                        <th style="width: 70px; text-align: right;">Page</th>
                    </tr>
                </thead>
                <tbody>
                    <tr><td>Chapter 1</td><td><strong>Introduction & Design Philosophy:</strong> Purpose, privacy, and zero-default principles.</td><td style="text-align: right;">Page 3</td></tr>
                    <tr><td>Chapter 2</td><td><strong>Getting Started & Navigation:</strong> Home Dashboard layout and 5 bottom tabs.</td><td style="text-align: right;">Page 4</td></tr>
                    <tr><td>Chapter 3</td><td><strong>Workout Routines: Initial State:</strong> Clean empty state with zero dummy routines.</td><td style="text-align: right;">Page 5</td></tr>
                    <tr><td>Chapter 4</td><td><strong>Routine Blueprint Authoring:</strong> Crafting splits, exercises, target days, and target RPE.</td><td style="text-align: right;">Page 6</td></tr>
                    <tr><td>Chapter 5</td><td><strong>Active Routine Management:</strong> Launching workouts, adherence card, and routine options.</td><td style="text-align: right;">Page 7</td></tr>
                    <tr><td>Chapter 6</td><td><strong>Exercise Catalog & Filtering:</strong> 1,324+ movement database with muscle filters.</td><td style="text-align: right;">Page 8</td></tr>
                    <tr><td>Chapter 7</td><td><strong>Movement Details & Execution:</strong> Looping visual animation, anatomy, and PR history.</td><td style="text-align: right;">Page 9</td></tr>
                    <tr><td>Chapter 8</td><td><strong>Custom Exercise Creation:</strong> Adding custom movements directly to local SQLite.</td><td style="text-align: right;">Page 10</td></tr>
                    <tr><td>Chapter 9</td><td><strong>Active Workout Logging & Timer:</strong> Set logging, previous references, and rest timer.</td><td style="text-align: right;">Page 11</td></tr>
                    <tr><td>Chapter 10</td><td><strong>Flexible Weight & Reps Input:</strong> Unconstrained keypad with decimal load precision.</td><td style="text-align: right;">Page 12</td></tr>
                    <tr><td>Chapter 11</td><td><strong>Session Summary & Evaluation:</strong> Volume tonnage, net duration, and personal records.</td><td style="text-align: right;">Page 13</td></tr>
                    <tr><td>Chapter 12</td><td><strong>Progress Analytics & Estimated 1RM:</strong> Strength curves and weekly tonnage volume.</td><td style="text-align: right;">Page 14</td></tr>
                    <tr><td>Chapter 13</td><td><strong>Athlete Profile & Lifetime Stats:</strong> Career statistics and profile navigation.</td><td style="text-align: right;">Page 15</td></tr>
                    <tr><td>Chapter 14</td><td><strong>Body Measures & Physical Tracking:</strong> Body weight and metrics with zero dummy seed data.</td><td style="text-align: right;">Page 16</td></tr>
                    <tr><td>Chapter 15</td><td><strong>Application Settings & Preferences:</strong> Units (kg/lbs), pure OLED dark mode, and language.</td><td style="text-align: right;">Page 17</td></tr>
                    <tr><td>Chapter 16</td><td><strong>Daily Workout Reminders:</strong> Exact alarm scheduling and active training day triggers.</td><td style="text-align: right;">Page 18</td></tr>
                    <tr><td>Chapter 17</td><td><strong>Custom Notification Time Keypad:</strong> Modern keypad for exact hour and minute setting.</td><td style="text-align: right;">Page 19</td></tr>
                    <tr><td>Chapter 18</td><td><strong>Data Backup, Restore & Safety:</strong> JSON export/import and 7-day reminder cooldown.</td><td style="text-align: right;">Page 20</td></tr>
                    <tr><td>Chapter 19</td><td><strong>Practical Gym Training Workflow:</strong> Step-by-step gym execution from start to finish.</td><td style="text-align: right;">Page 21</td></tr>
                    <tr><td>Chapter 20</td><td><strong>Troubleshooting & FAQ:</strong> Android battery optimization, background timers, and recovery.</td><td style="text-align: right;">Page 22</td></tr>
                    <tr><td>Chapter 21</td><td><strong>Reference Glossary & RPE Scale:</strong> Standardized Borg RPE 1–10 table and fitness terms.</td><td style="text-align: right;">Page 23</td></tr>
                    <tr><td>Chapter 22</td><td><strong>Technical Specifications & Release:</strong> Architecture, OS support, SQLite schema, and license.</td><td style="text-align: right;">Page 24</td></tr>
                </tbody>
            </table>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 2</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 3: INTRODUCTION & PHILOSOPHY
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-intro">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 1: Introduction & Design Philosophy</span>
            <span>Page 3</span>
        </div>
        <div class="content">
            <h1>Chapter 1: Introduction & Design Philosophy</h1>
            <p class="lead">KumaLift is an offline-first weight training tracker designed for strength athletes and lifters prioritizing progressive overload, complete privacy, and zero operational friction.</p>

            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <h2>Core Design Principles</h2>
                    <p>Unlike conventional workout trackers cluttered with social media feeds, algorithmic feeds, and mandatory cloud logins, KumaLift adheres to four strict principles:</p>
                    
                    <ul class="step-list" style="margin-left: 14px;">
                        <li><strong>Zero Default Assumptions:</strong> No pre-populated dummy routines, fake weight logs, or pre-seeded templates. Every routine and measurement is intentionally created by the lifter.</li>
                        <li><strong>Local-First Sovereignty:</strong> All workout histories, custom exercises, and anthropometric data reside strictly on the user's device in an encrypted SQLite database. Zero external servers or tracking telemetry.</li>
                        <li><strong>Gym-Floor Ergonomics:</strong> High-contrast OLED dark theme minimizes battery consumption and ensures effortless legibility in dim gym environments. Massive tap targets facilitate input between heavy sets.</li>
                        <li><strong>Progressive Overload Tracking:</strong> Real-time display of previous set performances, automatic 1RM calculation via Brzycki formulas, and rest countdown timers keep sessions focused on strength progression.</li>
                    </ul>
                </div>

                <div>
                    <h2>Technical Architecture Overview</h2>
                    <p>KumaLift is built on modern, industrial-grade multiplatform technologies to guarantee instantaneous responsiveness and zero reliance on network connectivity:</p>

                    <table class="clean-table">
                        <thead>
                            <tr>
                                <th>Component</th>
                                <th>Implementation Detail</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td>UI Framework</td><td>Compose Multiplatform (Declarative UI)</td></tr>
                            <tr><td>Local Database</td><td>SQLite via SQLDelight (Schema version 1)</td></tr>
                            <tr><td>Alarm Scheduling</td><td>Android AlarmManager (SCHEDULE_EXACT_ALARM)</td></tr>
                            <tr><td>Movement Database</td><td>1,324+ Pre-compiled Technical Exercises</td></tr>
                            <tr><td>Media Assets</td><td>Locally cached high-efficiency WebP animations</td></tr>
                            <tr><td>Data Portability</td><td>Standardized JSON & CSV import/export</td></tr>
                        </tbody>
                    </table>

                    <div class="note-box" style="margin-top: 10px;">
                        <strong>Quick Start Advice:</strong> For the most consistent training experience, begin by authoring your routine in Chapter 4, or start an empty workout directly from the Home Dashboard in Chapter 2.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 3</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 4: HOME DASHBOARD & NAVIGATION
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-dashboard">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 2: Getting Started & Navigation</span>
            <span>Page 4</span>
        </div>
        <div class="content">
            <h1>Chapter 2: Getting Started & Navigation</h1>
            <p class="lead">The Home Dashboard serves as your central command deck upon launching KumaLift, displaying weekly training adherence, immediate workout launchers, and career totals.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['01']}" alt="Home Dashboard">
                    <div class="caption">Figure 1: Home Dashboard Screen & Bottom Navigation Bar</div>
                </div>

                <div>
                    <h2>Key Interface Elements</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Athlete Greeting:</span>
                                <span class="callout-desc">Displays the personalized greeting and active date. Tap the profile avatar to jump directly to account settings.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Weekly Schedule Tracker:</span>
                                <span class="callout-desc">7-day interactive bar showing scheduled training days, completed workouts, and remaining planned rest days for the current calendar week.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Quick Start Workout Button:</span>
                                <span class="callout-desc">One-tap launcher to immediately begin an open, unstructured workout session without requiring a pre-authored routine.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Recent Activity Feed:</span>
                                <span class="callout-desc">Chronological cards of recently concluded workouts detailing completion date, elapsed duration, total volume tonnage, and key personal records.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Global 5-Tab Navigation:</span>
                                <span class="callout-desc">Persistent bottom navigation bar providing seamless switching between Home, Workout Routines, Exercise Catalog, Progress Analytics, and Profile.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Primary Navigation Destinations</h3>
                    <table class="clean-table">
                        <thead>
                            <tr>
                                <th>Tab Destination</th>
                                <th>Primary Operational Purpose</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td>Home</td><td>Dashboard overview, adherence tracker, and quick workout launcher.</td></tr>
                            <tr><td>Workouts</td><td>Custom training routines, split blueprints, and program adherence.</td></tr>
                            <tr><td>Exercises</td><td>Comprehensive movement library, visual guides, and custom exercises.</td></tr>
                            <tr><td>Progress</td><td>Volume progression charts, estimated 1RM trends, and workout history.</td></tr>
                            <tr><td>Profile</td><td>Athlete statistics, body measures, and system settings.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 4</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 5: WORKOUT ROUTINES: INITIAL STATE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-routines-empty">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 3: Workout Routines — Initial State</span>
            <span>Page 5</span>
        </div>
        <div class="content">
            <h1>Chapter 3: Workout Routines — Initial State</h1>
            <p class="lead">KumaLift enforces a clean, zero-default policy: no artificial sample routines or cluttered dummy templates are pre-installed upon fresh setup.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['02']}" alt="Workout Routines Initial State">
                    <div class="caption">Figure 2: Empty State Display for the Workouts Tab</div>
                </div>

                <div>
                    <h2>Clean Slate Architecture</h2>
                    <p>Every athlete possesses unique biomechanics, equipment availability, and progression goals. Pre-seeded routine templates often lead to unnecessary clutter and confusion. When first visiting the Workouts tab, KumaLift presents a pristine canvas ready for your custom programming.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Header Action Button:</span>
                                <span class="callout-desc">Quick-access button to create a new routine directly from the top navigation bar at any time.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Weekly Adherence Card:</span>
                                <span class="callout-desc">Tracks completion progress against your weekly training commitment (e.g., 0 / 4 sessions complete this week). Updates dynamically as workouts finish.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Program Filter Pill:</span>
                                <span class="callout-desc">Filters your authored routine blueprints by training program or split category once multiple routines are added.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Empty State Call to Action:</span>
                                <span class="callout-desc">Clear informative prompt indicating no routines currently exist, accompanied by a prominent button to craft your first routine blueprint.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Advantages of Clean Slate Design</h3>
                    <ul class="step-list">
                        <li><strong>No Clutter:</strong> Prevents scrolling past dozen unused generic routines to locate your actual training program.</li>
                        <li><strong>Intentional Training:</strong> Encourages deliberate programming tailored to your personal frequency, split preference, and available gym equipment.</li>
                        <li><strong>Zero Residual Data:</strong> Guarantees your SQLite database contains only records that you intentionally authored.</li>
                    </ul>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 5</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 6: ROUTINE BLUEPRINT AUTHORING
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-create-routine">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 4: Routine Blueprint Authoring</span>
            <span>Page 6</span>
        </div>
        <div class="content">
            <h1>Chapter 4: Routine Blueprint Authoring</h1>
            <p class="lead">A routine blueprint acts as a reusable template defining your training split, designated training days, exercise sequence, and target set parameters.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['03']}" alt="Create Routine Screen">
                    <div class="caption">Figure 3: Routine Creation & Configuration Interface</div>
                </div>

                <div>
                    <h2>Authoring Interface Elements</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Routine Title Field:</span>
                                <span class="callout-desc">Descriptive name for your training day (e.g., "Hypertrophy Push A", "Heavy Lower Body", or "Upper Body Focus").</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Split Category Selector:</span>
                                <span class="callout-desc">Select the training split classification (Push, Pull, Legs, Upper, Lower, Full Body) for automated volume grouping and analytics.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Target Day Selector:</span>
                                <span class="callout-desc">Assign intended weekly schedule days (Mon through Sun) to enable automated calendar tracking on your Home Dashboard.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Add Exercise Action:</span>
                                <span class="callout-desc">Opens the exercise catalog to select movements and append them to this routine blueprint in sequence.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Save Routine Button:</span>
                                <span class="callout-desc">Validates routine parameters and commits the blueprint permanently to the local SQLite database.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Step-by-Step Blueprint Creation</h3>
                    <ol class="step-list">
                        <li>Navigate to the <strong>Workouts</strong> tab and tap <strong>Create Routine</strong>.</li>
                        <li>Enter a descriptive name and select your target split category.</li>
                        <li>Toggle the specific days of the week you plan to execute this routine.</li>
                        <li>Tap <strong>+ Add Exercise</strong>, search or filter through the catalog, and select movements.</li>
                        <li>Review exercise ordering, adjust target sets if desired, and tap <strong>Save Routine</strong>.</li>
                    </ol>

                    <div class="note-box">
                        <strong>Flexibility Note:</strong> Blueprints are non-destructive guidelines. During an active gym session, you can freely add, remove, or substitute exercises without modifying your underlying saved blueprint.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 6</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 7: ACTIVE ROUTINE MANAGEMENT
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-routines-list">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 5: Active Routine Management</span>
            <span>Page 7</span>
        </div>
        <div class="content">
            <h1>Chapter 5: Active Routine Management</h1>
            <p class="lead">Manage your saved routine library, monitor weekly program adherence, and launch structured workout sessions with a single tap.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['04']}" alt="Routines List">
                    <div class="caption">Figure 4: Workouts Tab with Populated Routine Library</div>
                </div>

                <div>
                    <h2>Routine Card Components</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Program Adherence Progress:</span>
                                <span class="callout-desc">Visual progress bar reflecting the ratio of completed workouts against your weekly routine target.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Routine Identity Card:</span>
                                <span class="callout-desc">Card displaying routine title, split badge, total movements included, estimated duration, and scheduled days.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Context Menu:</span>
                                <span class="callout-desc">Three-dot overflow menu allowing you to Edit routine parameters, Duplicate for variations, or Delete obsolete blueprints.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Start Workout Launcher:</span>
                                <span class="callout-desc">Instantly loads the routine's exercises into the live Workout Logger, starts the session timer, and prepares set entries.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Managing Multiple Routines</h3>
                    <p>Lifters following multi-day splits (such as Push-Pull-Legs or Upper-Lower) can organize distinct blueprints for each day. Filter pills at the top allow quick narrowing by split type when managing large libraries.</p>

                    <div class="note-box">
                        <strong>Timer Integrity Guarantee:</strong> Tapping <em>Start Workout</em> is the exclusive action that initiates the session timer. Browsing or editing routines never records unwanted time entries in your history.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 7</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 8: EXERCISE CATALOG & FILTERING
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-catalog">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 6: Exercise Catalog & Filtering</span>
            <span>Page 8</span>
        </div>
        <div class="content">
            <h1>Chapter 6: Exercise Catalog & Filtering</h1>
            <p class="lead">KumaLift includes an extensive local library containing over 1,324 movements, indexed by muscle anatomy, movement type, and required gym equipment.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['05']}" alt="Exercise Catalog">
                    <div class="caption">Figure 5: Exercise Catalog with Multi-Tier Anatomical Filters</div>
                </div>

                <div>
                    <h2>Search & Filter Tools</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Search Bar:</span>
                                <span class="callout-desc">Real-time keyword search querying exercise names, alternate terms, and anatomical targets with instant typing response.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Muscle Group Chips:</span>
                                <span class="callout-desc">Horizontally scrollable category pills (Chest, Back, Shoulders, Arms, Legs, Core, Cardio) for instantaneous anatomical filtering.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Movement Item Card:</span>
                                <span class="callout-desc">Displays exercise name, primary muscle group badge, equipment icon, and preview thumbnail. Tap to inspect full movement details.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Custom Exercise Button:</span>
                                <span class="callout-desc">Direct header action to register your own custom movements and specialty gym machines not present in the default catalog.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Offline Search Engine Efficiency</h3>
                    <p>All 1,324+ movements and metadata are indexed in local SQLite tables with normalized full-text search. Queries execute in less than 5 milliseconds without querying external network services or consuming cellular data.</p>

                    <div class="note-box">
                        <strong>Gym Substitution Tip:</strong> If equipment is occupied during peak hours, use the muscle chip filter to quickly find an equivalent movement targeting the identical primary muscle group.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 8</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 9: MOVEMENT DETAILS & EXECUTION
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-exercise-detail">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 7: Movement Details & Execution Technique</span>
            <span>Page 9</span>
        </div>
        <div class="content">
            <h1>Chapter 7: Movement Details & Execution Technique</h1>
            <p class="lead">Examine technical biomechanical cues, inspect visual demonstration loops, and review your historical strength achievements for any movement.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['06']}" alt="Exercise Detail">
                    <div class="caption">Figure 6: Movement Detail Screen with Visual Demonstration</div>
                </div>

                <div>
                    <h2>Detail Screen Components</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Visual Animation Player:</span>
                                <span class="callout-desc">Fluid, continuous visual demonstration loop displaying proper concentric and eccentric movement phases. Tap to pause or restart.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Muscle Target Badges:</span>
                                <span class="callout-desc">Highlights primary agonist muscles alongside secondary synergist and stabilizer muscle groups engaged throughout the range of motion.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Technical Execution Cues:</span>
                                <span class="callout-desc">Clear, step-by-step biomechanical instructions covering setup, posture, breathing mechanics, and bar path execution.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Personal Record History:</span>
                                <span class="callout-desc">Records your all-time heaviest load, best estimated 1RM, highest volume set, and date of achievement for this specific movement.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Optimized Asset Delivery</h3>
                    <p>Demonstration animations are compressed into modern WebP sequences embedded locally inside the application binary. This guarantees instantaneous playback without buffering delays, battery drain, or network bandwidth usage.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 9</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 10: CUSTOM EXERCISE CREATION
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-custom-exercise">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 8: Custom Exercise Creation</span>
            <span>Page 10</span>
        </div>
        <div class="content">
            <h1>Chapter 8: Custom Exercise Creation</h1>
            <p class="lead">Register custom gym movements, specialized cable variations, or unique gym equipment to ensure your workout tracking reflects your exact training routine.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['06b']}" alt="Create Custom Exercise">
                    <div class="caption">Figure 7: Custom Movement Authoring Modal</div>
                </div>

                <div>
                    <h2>Custom Exercise Form Fields</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Exercise Name Input:</span>
                                <span class="callout-desc">Enter the title of your custom variation (e.g., "Incline Hammer Grip DB Bench Press" or "Belt Squat Machine").</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Target Muscle Category:</span>
                                <span class="callout-desc">Select the primary muscle group to ensure the movement correctly populates anatomical filter chips and volume analytics.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Equipment Type:</span>
                                <span class="callout-desc">Categorize by equipment type: Barbell, Dumbbell, Machine, Cable, Bodyweight, or Band.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Custom Notes & Cues:</span>
                                <span class="callout-desc">Optional field to record pin settings, seat height, handle attachments, or personal execution cues for future reference.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Save Movement Action:</span>
                                <span class="callout-desc">Permanently commits the new exercise into your local database. It immediately becomes searchable and loggable.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Database Parity</h3>
                    <p>Custom exercises behave identically to built-in movements. They support full 1RM curve calculation, volume tonnage summation, personal record tracking, and are preserved across data backups.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 10</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 11: ACTIVE WORKOUT LOGGING & TIMERS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-logger">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 9: Active Workout Logging & Live Timers</span>
            <span>Page 11</span>
        </div>
        <div class="content">
            <h1>Chapter 9: Active Workout Logging & Live Timers</h1>
            <p class="lead">The Workout Logger is the core active screen during gym training, featuring an exact stopwatch timer, set checklist, and automated rest countdowns.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['08']}" alt="Active Workout Logger">
                    <div class="caption">Figure 8: Active Workout Logger with Rest Interval Timer</div>
                </div>

                <div>
                    <h2>Logger Table & Controls</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Active Stopwatch Timer:</span>
                                <span class="callout-desc">Tracks exact elapsed workout duration. Begins strictly upon tapping Start Workout and pauses accurately if suspended.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Rest Interval Countdown:</span>
                                <span class="callout-desc">Dynamic countdown banner initiated immediately when a set is marked complete. Configurable in settings (e.g., 60s, 90s, 120s, 180s).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Previous Performance Reference:</span>
                                <span class="callout-desc">Displays the exact weight and reps logged in the preceding workout session, serving as your immediate progressive overload benchmark.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Flexible Load & Reps Cells:</span>
                                <span class="callout-desc">Tap either cell to trigger the unconstrained numeric keypad for precise load entry (including decimal fractions) and repetition counts.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Set Completion Checkbox:</span>
                                <span class="callout-desc">Tap to complete a set. Changes row state to highlighted green, records volume, and automatically starts the rest interval countdown.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Consistent Column Alignment</h3>
                    <p>The set table header and data rows maintain rigorous vertical alignment across all device displays: <strong>Set</strong>, <strong>Prev</strong>, <strong>KG</strong>, <strong>Reps</strong>, <strong>RPE</strong>, and <strong>Check</strong> align perfectly without overlapping or text truncations.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 11</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 12: FLEXIBLE WEIGHT & REPS INPUT
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-custom-inputs">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 10: Flexible Weight & Reps Input</span>
            <span>Page 12</span>
        </div>
        <div class="content">
            <h1>Chapter 10: Flexible Weight & Reps Input</h1>
            <p class="lead">Unconstrained numeric dialogs supporting arbitrary weight values with decimal precision and free repetition counts.</p>

            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['09']}" alt="Dialog Edit Weight">
                        <div class="caption">Figure 9: Custom Weight Input Dialog (KG)</div>
                    </div>
                    <h3 style="margin-top: 6px;">Weight Load Input (KG)</h3>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div><span class="callout-title">Free Decimal Entry:</span> <span class="callout-desc">Supports integers and decimal fractions (e.g., <code>32.5</code>, <code>17.25</code>, <code>67.5</code>). Valid range: 0.1 to 999.9 kg.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div><span class="callout-title">Quick Adjust Buttons:</span> <span class="callout-desc">Convenient stepper buttons (<code>-5</code>, <code>-1</code>, <code>+1</code>, <code>+5</code>) for rapid single-tap weight adjustments.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div><span class="callout-title">Save Button:</span> <span class="callout-desc">Applies the entered load immediately to the active set row and recalculates volume.</span></div>
                        </li>
                    </ul>
                </div>

                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['10']}" alt="Dialog Edit Reps">
                        <div class="caption">Figure 10: Custom Repetitions Input Dialog</div>
                    </div>
                    <h3 style="margin-top: 6px;">Repetition Count Input</h3>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div><span class="callout-title">Direct Reps Entry:</span> <span class="callout-desc">Log exact completed repetitions (1 to 999 reps) freely without restrictive preset increment limitations.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div><span class="callout-title">Quick Stepper Pills:</span> <span class="callout-desc">Increment or decrement repetitions quickly with dedicated step pills without opening the full system keyboard.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div><span class="callout-title">Save Button:</span> <span class="callout-desc">Commits the completed repetition count directly into your active workout session ledger.</span></div>
                        </li>
                    </ul>
                </div>
            </div>

            <div class="note-box" style="margin-top: 8px;">
                <strong>Importance of Decimal Micro-Loading:</strong> In modern progressive overload programming, micro-loading with fractional plates (e.g., 1.25 kg or 0.5 kg increments) is essential for sustained strength progression on compound lifts without inducing joint strain.
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 12</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 13: SESSION SUMMARY & PERSONAL RECORDS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-summary">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 11: Session Summary & PR Records</span>
            <span>Page 13</span>
        </div>
        <div class="content">
            <h1>Chapter 11: Session Summary & PR Records</h1>
            <p class="lead">Upon tapping Finish Workout, KumaLift compiles a comprehensive session evaluation detailing total volume tonnage, net training time, and new personal records.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['12']}" alt="Workout Summary">
                    <div class="caption">Figure 10: Post-Workout Evaluation & Lifetime PR Summary</div>
                </div>

                <div>
                    <h2>Summary Screen Analytics</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Total Lifted Volume (Tonnage):</span>
                                <span class="callout-desc">Aggregate load multiplied by completed repetitions across all completed sets (e.g., 2,820 kg total volume).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Net Workout Duration:</span>
                                <span class="callout-desc">Clean training duration recorded between session start and conclusion, excluding prolonged pauses.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Total Completed Sets:</span>
                                <span class="callout-desc">Sum of verified completed sets logged during the training session.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Personal Record (PR) Achievements:</span>
                                <span class="callout-desc">Special highlight card showcasing new milestone records: Heaviest Weight, Best Estimated 1RM, or Maximum Set Volume.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Save to Career History Button:</span>
                                <span class="callout-desc">Finalizes session persistence into SQLite database and returns the athlete cleanly to the Home Dashboard.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Automated PR Detection Engine</h3>
                    <p>KumaLift automatically compares every completed set against your historical database records. When a set exceeds your previous best weight, best estimated 1RM, or highest rep-volume, the app awards an official PR badge and records the milestone date.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 13</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 14: PROGRESS ANALYTICS & ESTIMATED 1RM
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-analytics">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 12: Progress Analytics & 1RM Curves</span>
            <span>Page 14</span>
        </div>
        <div class="content">
            <h1>Chapter 12: Progress Analytics & 1RM Curves</h1>
            <p class="lead">Visualize long-term strength progression, inspect estimated 1-Rep Max curves, and analyze weekly volume tonnage distribution across muscle groups.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['13']}" alt="Progress Analytics">
                    <div class="caption">Figure 11: Progress Analytics & Estimated 1RM Strength Curve</div>
                </div>

                <div>
                    <h2>Analytics Dashboard Components</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Analytics Tab Navigation:</span>
                                <span class="callout-desc">Toggle between Progression Curve Charts, Weekly Volume Distributions, and Chronological Log History feeds.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Exercise Selector Dropdown:</span>
                                <span class="callout-desc">Select any movement in your history to generate an isolated, high-resolution strength trajectory curve.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Estimated 1RM Trajectory Graph:</span>
                                <span class="callout-desc">Time-series line chart tracking theoretical one-rep maximums derived from sub-maximal sets, revealing strength trends over time.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Weekly Volume Tonnage Bar Chart:</span>
                                <span class="callout-desc">Histogram depicting cumulative lifted tonnage per training week to audit training volume and manage fatigue.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>The Brzycki 1RM Formula</h3>
                    <p>To safely estimate maximum strength without risking injury under maximal single-rep loads, KumaLift computes 1RM values using the validated Brzycki formula:</p>
                    <div style="background: #111827; border: 1px solid #1E293B; border-radius: 6px; padding: 6px 12px; font-family: monospace; font-size: 8.5pt; color: #38BDF8; margin: 4px 0 8px 0; text-align: center;">
                        1RM = Weight / (1.0278 - (0.0278 * Reps))
                    </div>
                    <p style="font-size: 7.8pt; color: #64748B;">Accurate for repetition ranges between 1 and 10 reps performed with solid technical execution.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 14</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 15: ATHLETE PROFILE & STATS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-profile">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 13: Athlete Profile & Lifetime Stats</span>
            <span>Page 15</span>
        </div>
        <div class="content">
            <h1>Chapter 13: Athlete Profile & Lifetime Stats</h1>
            <p class="lead">The Profile Dashboard aggregates your lifetime training volume, career workout frequency, and provides direct access to body measurements and settings.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['14']}" alt="Profile Dashboard">
                    <div class="caption">Figure 12: Athlete Profile & Lifetime Training Career Statistics</div>
                </div>

                <div>
                    <h2>Profile Screen Elements</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Athlete Identity Card:</span>
                                <span class="callout-desc">Displays customized username, athlete tier status, and member join date. Editable in settings.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Lifetime Career Tonnage:</span>
                                <span class="callout-desc">Cumulative sum of every kilogram lifted throughout your entire training history with KumaLift.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Total Workouts Completed:</span>
                                <span class="callout-desc">Total count of concluded training sessions logged since initial application installation.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Body Measures Shortcut:</span>
                                <span class="callout-desc">Dedicated navigation card opening the body anthropometry and weight tracking modal.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Settings Navigation Icon:</span>
                                <span class="callout-desc">Top-right gear icon granting direct access to application preferences, notification schedules, and data backup.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Local Identity Integrity</h3>
                    <p>Your athlete profile requires no external cloud authentication, Google sign-in, or social profile linkage. Your identity details are stored purely as local key-value preferences within Android SharedPreferences and SQLite.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 15</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 16: BODY MEASUREMENTS (MEASURES)
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-measures">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 14: Body Measures & Physical Tracking</span>
            <span>Page 16</span>
        </div>
        <div class="content">
            <h1>Chapter 14: Body Measures & Physical Tracking</h1>
            <p class="lead">Track changes in body mass, body fat percentage, and muscle circumferences over time with a clean, zero-default measurement ledger.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['15']}" alt="Measures Modal">
                    <div class="caption">Figure 13: Body Measures Modal with Zero Dummy Seed Data</div>
                </div>

                <div>
                    <h2>Clean Measurement Architecture</h2>
                    <p>KumaLift ships with zero pre-seeded body measurements. No fake sample weights or mock circumferences clutter your ledger. You populate and maintain your own metrics from day one.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Measurement Date Selector:</span>
                                <span class="callout-desc">Calendar date picker to record measurements on specific historical dates or the current day.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Body Weight Input (KG / LBS):</span>
                                <span class="callout-desc">Primary weight entry supporting decimal precision (e.g., 78.45 kg) matching your home scale.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Body Fat Percentage (%):</span>
                                <span class="callout-desc">Optional caliper or DEXA scan body fat percentage metric for body composition tracking.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Circumference Metrics:</span>
                                <span class="callout-desc">Dedicated fields for Chest, Arms, Waist, and Thigh measurements to track muscular hypertrophy.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Save Measurement Action:</span>
                                <span class="callout-desc">Commits the physical measurement entry into your chronological SQLite progress history table.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Tracking Recommendation:</strong> For optimal consistency, measure body weight once per week immediately after waking, prior to breakfast or fluid intake.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 16</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 17: APPLICATION SETTINGS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-settings">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 15: Application Settings & Preferences</span>
            <span>Page 17</span>
        </div>
        <div class="content">
            <h1>Chapter 15: Application Settings & Preferences</h1>
            <p class="lead">Configure application units, visual interface themes, language preferences, and manage local data backup protocols.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['16']}" alt="Settings Main">
                    <div class="caption">Figure 14: Application Settings & Preference Management</div>
                </div>

                <div>
                    <h2>Settings Categories</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Weight Unit Toggle:</span>
                                <span class="callout-desc">Switch seamlessly between Kilograms (kg) and Pounds (lbs). All historical logs and calculations convert accurately.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Language Selector:</span>
                                <span class="callout-desc">Select between English and Bahasa Indonesia. The UI dynamically reloads strings across all navigation tabs.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Default Rest Interval:</span>
                                <span class="callout-desc">Set your preferred default countdown duration (60s, 90s, 120s, 180s) automatically triggered when checking off a set.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Notification Scheduling:</span>
                                <span class="callout-desc">Submenu to configure daily training reminders with exact hardware alarm precision.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Backup & Restore Management:</span>
                                <span class="callout-desc">Dedicated portal to export your entire SQLite database to JSON and restore from previous backups.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Pure OLED Dark Theme</h3>
                    <p>KumaLift features an intentional pitch-black (#0B0F17) background optimized for modern AMOLED and OLED smartphone displays. True black pixels emit zero light, significantly extending battery lifespan during prolonged 90-minute gym sessions.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 17</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 18: NOTIFICATION SCHEDULING
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-notifications">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 16: Accurate Notification Scheduling</span>
            <span>Page 18</span>
        </div>
        <div class="content">
            <h1>Chapter 16: Accurate Notification Scheduling</h1>
            <p class="lead">Configure exact workout reminder notifications engineered to fire within sub-minute precision without being silenced by aggressive OS battery optimization.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['17']}" alt="Notification Settings">
                    <div class="caption">Figure 15: Exact Notification Scheduling Configuration</div>
                </div>

                <div>
                    <h2>Scheduling Controls</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Master Reminder Toggle:</span>
                                <span class="callout-desc">Global toggle switch enabling or disabling all workout reminder notifications across the application.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Training Day Checklist:</span>
                                <span class="callout-desc">Toggle specific days of the week (Mon through Sun) to receive reminders only on your active training days.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Reminder Time Selector:</span>
                                <span class="callout-desc">Tap to select preset reminder times or open the custom numeric keypad for exact hour and minute scheduling.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Exact Alarm Permission Status:</span>
                                <span class="callout-desc">Direct indicator confirming Android AlarmManager exact alarm permissions (SCHEDULE_EXACT_ALARM) are active.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Hardware-Level Precision Guarantee</h3>
                    <p>Conventional apps rely on generic background workers that Android Doze Mode defers by 15–30 minutes to conserve power. KumaLift utilizes Android's native <code>AlarmManager.setExactAndAllowWhileIdle()</code> API, ensuring your notification triggers at the exact appointed minute, even while the device is in deep sleep.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 18</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 19: CUSTOM TIME KEYPAD DIALOG
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-reminder-keypad">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 17: Custom Notification Time Keypad</span>
            <span>Page 19</span>
        </div>
        <div class="content">
            <h1>Chapter 17: Custom Notification Time Keypad</h1>
            <p class="lead">Specify exact notification alarm times with a clean, high-precision numeric keypad dialog designed for rapid 24-hour time entry.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['18']}" alt="Custom Reminder Dialog">
                    <div class="caption">Figure 16: Interactive Numeric Keypad Time Selector Modal</div>
                </div>

                <div>
                    <h2>Custom Time Keypad Controls</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Digital Hours & Minutes Indicator:</span>
                                <span class="callout-desc">Dual interactive digital blocks displaying Hours (00–23) and Minutes (00–59) in standard 24-hour format.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Ergonomic Numeric Keypad:</span>
                                <span class="callout-desc">Direct 0–9 keypad with dedicated 00 and backspace keys, allowing effortless input of any exact time (e.g., 07:15, 18:45).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Save Button:</span>
                                <span class="callout-desc">Commits the chosen time to local preferences, schedules the AlarmManager intent, and dismisses the dialog.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Daily Notification Message Rotation</h3>
                    <p>KumaLift features 5 motivational reminder messages randomly rotated across scheduled training days:</p>
                    <ul class="step-list" style="margin-left: 14px;">
                        <li>"Time to train! Build consistency and reach your goals today."</li>
                        <li>"Your body is ready to grow. Don't skip today's workout!"</li>
                        <li>"Discipline is the bridge between goals and accomplishment."</li>
                        <li>"Every set brings you closer to your potential. Let's lift!"</li>
                        <li>"Workout time! Make time for yourself and crush your session."</li>
                    </ul>

                    <div class="note-box">
                        <strong>Duplicate Protection:</strong> The scheduler maintains persistent alarm IDs. Updating your scheduled time automatically cancels any preexisting alarm before registering the new trigger, preventing phantom duplicate alerts.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 19</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 20: DATA BACKUP & SAFETY POLICY
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-backup">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 18: Data Backup, Restore & Safety</span>
            <span>Page 20</span>
        </div>
        <div class="content">
            <h1>Chapter 18: Data Backup, Restore & Safety</h1>
            <p class="lead">Protect your workout history with portable offline JSON exports, safe restore workflows, and an intelligent weekly reminder cooldown policy.</p>

            <div class="feature-layout">
                <div class="screenshot-frame">
                    <img src="{imgs['19']}" alt="Backup Management">
                    <div class="caption">Figure 17: Backup & Data Recovery Portal</div>
                </div>

                <div>
                    <h2>Data Management Tools</h2>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Export Database Backup:</span>
                                <span class="callout-desc">Packages your complete SQLite database (routines, workout logs, PRs, custom exercises, body measures) into a structured JSON file.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Import & Restore Database:</span>
                                <span class="callout-desc">Safely restores workout history from a previously exported JSON backup file on a new or reset device.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Last Backup Timestamp:</span>
                                <span class="callout-desc">Prominently displays the exact date and time of your most recent successful backup export.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>7-Day Backup Reminder Cooldown</h3>
                    <p>Unlike intrusive apps that interrupt every launch with nagging backup popups, KumaLift enforces a strict 7-day cooldown policy:</p>
                    <ul class="step-list">
                        <li>The reminder dialog appears at most <strong>once every 7 days</strong>.</li>
                        <li>Cooldown timing resets only when the user <em>interacts</em> with the dialog (tapping Backup Now or Remind Later).</li>
                        <li>Timestamps are persisted in Android SharedPreferences, surviving app restarts and device reboots.</li>
                    </ul>

                    <div class="note-box">
                        <strong>Data Sovereignty Guarantee:</strong> Exported JSON files are completely unencrypted standard text. You can inspect your records in any text editor, import them into spreadsheet software, or store them in your personal cloud drive.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 20</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 21: PRACTICAL GYM TRAINING WORKFLOW
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-workflow">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 19: Practical Gym Training Workflow</span>
            <span>Page 21</span>
        </div>
        <div class="content">
            <h1>Chapter 19: Practical Gym Training Workflow</h1>
            <p class="lead">A step-by-step chronological walkthrough illustrating how to efficiently operate KumaLift before, during, and after an intense gym session.</p>

            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <h2>Phase 1: Pre-Workout Preparation</h2>
                    <ol class="step-list">
                        <li><strong>Review Weekly Goal:</strong> Open the app; check the Home Dashboard adherence bar to verify today's planned split.</li>
                        <li><strong>Select Routine:</strong> Switch to the Workouts tab; locate today's routine blueprint (e.g., "Hypertrophy Push A").</li>
                        <li><strong>Initiate Session:</strong> Tap <strong>Start Workout</strong> on the routine card. The session stopwatch begins immediately.</li>
                    </ol>

                    <h2 style="margin-top: 10px;">Phase 2: During the Workout</h2>
                    <ol class="step-list">
                        <li><strong>Inspect Previous Benchmarks:</strong> Look at the <em>Prev</em> column to confirm your target weight and reps for Set 1.</li>
                        <li><strong>Execute & Record Set:</strong> Perform the set. Enter actual weight lifted and reps achieved via the numeric keypad.</li>
                        <li><strong>Check Off the Set:</strong> Tap the green checkmark checkbox. This logs the set and automatically initiates the rest countdown timer.</li>
                        <li><strong>Rest Discipline:</strong> Rest until the countdown timer completes; prepare your setup for the subsequent set.</li>
                    </ol>
                </div>

                <div>
                    <h2>Phase 3: Exercises & Substitutions</h2>
                    <ol class="step-list">
                        <li><strong>Substitute Equipment:</strong> If a machine is busy, tap <em>+ Add Exercise</em> to insert an alternate movement targeting the same muscle group.</li>
                        <li><strong>Reorder Movements:</strong> Adjust the exercise sequence dynamically as gym floor availability dictates.</li>
                        <li><strong>Optional RPE Rating:</strong> Record your Rate of Perceived Exertion (RPE 6–10) in the RPE cell to monitor fatigue.</li>
                    </ol>

                    <h2 style="margin-top: 10px;">Phase 4: Post-Workout Evaluation</h2>
                    <ol class="step-list">
                        <li><strong>Conclude Session:</strong> Tap <strong>Finish Workout</strong> in the upper right corner of the logger.</li>
                        <li><strong>Review Summary:</strong> Inspect your total volume tonnage, net active duration, and celebrated personal records (PRs).</li>
                        <li><strong>Commit to History:</strong> Tap <strong>Done</strong>. The session is written permanently to SQLite, and weekly adherence updates.</li>
                    </ol>

                    <div class="note-box" style="margin-top: 10px;">
                        <strong>Gym Pro Tip:</strong> If you accidentally close the app during a workout, simply relaunch KumaLift. Your active session and elapsed timer restore automatically without losing a single logged set.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 21</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 22: TROUBLESHOOTING & FAQ
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-troubleshooting">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 20: Troubleshooting & FAQ</span>
            <span>Page 22</span>
        </div>
        <div class="content">
            <h1>Chapter 20: Troubleshooting & FAQ</h1>
            <p class="lead">Concise solutions and authoritative answers to operational questions regarding Android permissions, battery optimization, and data preservation.</p>

            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <div class="qa-group">
                        <div class="qa-question">Q: Why did my workout reminder arrive late or not at all?</div>
                        <div class="qa-answer">A: Modern Android versions (Android 12+) aggressively restrict background alarms to conserve battery. Ensure KumaLift is granted "Alarms & Reminders" permission in Android Settings > Apps > KumaLift > Alarms. Additionally, set Battery usage to "Unrestricted" so Android Doze mode does not defer timers.</div>
                    </div>

                    <div class="qa-group">
                        <div class="qa-question">Q: Does the rest countdown timer pause if I switch to Spotify or lock my screen?</div>
                        <div class="qa-answer">A: No. The rest interval timer calculates remaining duration based on high-precision system epoch timestamps rather than UI ticks. Locking your screen or switching apps does not pause or alter the countdown.</div>
                    </div>

                    <div class="qa-group">
                        <div class="qa-question">Q: Why does the timer show 00:00 before I tap Start Workout?</div>
                        <div class="qa-answer">A: This is intentional. To ensure strict data integrity, the workout timer remains zeroed while inspecting or authoring routines. Time is recorded only while an active gym session is intentionally running.</div>
                    </div>
                </div>

                <div>
                    <div class="qa-group">
                        <div class="qa-question">Q: How do I transfer my workout history to a brand new Android phone?</div>
                        <div class="qa-answer">A: In KumaLift, go to Settings > Backup Management and tap <strong>Export Backup</strong>. Save or share the generated JSON file to Google Drive, email, or a USB drive. On your new device, install KumaLift, open Backup Management, and tap <strong>Import Backup</strong>.</div>
                    </div>

                    <div class="qa-group">
                        <div class="qa-question">Q: Can I delete or edit a set after I've already checked it complete?</div>
                        <div class="qa-answer">A: Yes. You can tap the green checkmark checkbox again to uncheck a completed set, tap the weight or reps cells to amend numbers, or swipe the row left to delete the set entirely.</div>
                    </div>

                    <div class="qa-group">
                        <div class="qa-question">Q: Does KumaLift require an active cellular or Wi-Fi internet connection?</div>
                        <div class="qa-answer">A: Absolutely not. KumaLift operates 100% offline. All 1,324+ exercises, demonstration animations, and logging engines are stored locally on your device.</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 22</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 23: GLOSSARY & RPE SCALE REFERENCE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-glossary">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 21: Reference Glossary & RPE Scale</span>
            <span>Page 23</span>
        </div>
        <div class="content">
            <h1>Chapter 21: Reference Glossary & RPE Scale</h1>
            <p class="lead">Standardized fitness terminology, intensity rating scale definitions, and core strength training formulas utilized throughout the application.</p>

            <h2>Standardized Rate of Perceived Exertion (RPE) Scale</h2>
            <p>RPE quantifies how close a set was taken to complete muscular failure. KumaLift utilizes the standardized 10-point scale based on Repetitions in Reserve (RIR):</p>

            <table class="clean-table" style="margin-top: 4px;">
                <thead>
                    <tr>
                        <th style="width: 50px;">RPE</th>
                        <th style="width: 70px;">RIR Equivalent</th>
                        <th>Perceived Exertion & Description</th>
                        <th>Training Application</th>
                    </tr>
                </thead>
                <tbody>
                    <tr><td>10</td><td>0 RIR</td><td>Maximal effort. Absolute failure; zero additional repetitions possible even with maximal strain.</td><td>Testing 1RM; final competition attempts.</td></tr>
                    <tr><td>9.5</td><td>0–1 RIR</td><td>Near maximal. Could not perform another full rep; might have performed a partial repetition.</td><td>Heavy strength peaking phases.</td></tr>
                    <tr><td>9</td><td>1 RIR</td><td>Extremely hard. Definitely 1 repetition remaining before complete muscular failure.</td><td>Primary hypertrophy & top strength sets.</td></tr>
                    <tr><td>8.5</td><td>1–2 RIR</td><td>Hard effort. Definitely 1 rep remaining, possibly 2 with complete technical focus.</td><td>Standard progressive overload training.</td></tr>
                    <tr><td>8</td><td>2 RIR</td><td>Solid working weight. Moderately heavy; 2 clean repetitions left in the tank.</td><td>Hypertrophy baseline & volume building.</td></tr>
                    <tr><td>7</td><td>3 RIR</td><td>Bar speed remains brisk and crisp. 3 repetitions remaining before muscular failure.</td><td>Speed work, dynamic effort, or back-off sets.</td></tr>
                    <tr><td>6</td><td>4+ RIR</td><td>Light effort. Warm-up weight or active recovery load. Minimal cardiovascular strain.</td><td>Specific warm-up sets & deload weeks.</td></tr>
                </tbody>
            </table>

            <h2 style="margin-top: 10px;">Fitness Terminology Glossary</h2>
            <div class="grid-2col">
                <div>
                    <p><strong>Progressive Overload:</strong> The systematic increase in stress placed upon the musculoskeletal system over time (via weight, reps, or volume) to stimulate muscular hypertrophy and strength adaptations.</p>
                    <p><strong>Volume Tonnage:</strong> Total physical work executed during a workout, computed as: <em>Weight (kg) × Completed Repetitions × Completed Sets</em>.</p>
                </div>
                <div>
                    <p><strong>Estimated 1RM (e1RM):</strong> Theoretical maximum single-rep load calculated mathematically from submaximal sets, enabling strength audits without maximum strain.</p>
                    <p><strong>Reps in Reserve (RIR):</strong> The number of clean repetitions an athlete could have completed before reaching absolute muscular failure on a given set.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 23</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 24: TECHNICAL SPECIFICATIONS & RELEASE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-specs">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Chapter 22: Technical Specifications & Release</span>
            <span>Page 24</span>
        </div>
        <div class="content">
            <h1>Chapter 22: Technical Specifications & Release</h1>
            <p class="lead">Comprehensive technical hardware specifications, software dependencies, storage requirements, and release notes for KumaLift Version 1.0.0.</p>

            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <h2>Platform & Hardware Specifications</h2>
                    <table class="clean-table">
                        <tbody>
                            <tr><td>Operating System</td><td>Android 8.0 (Oreo / API Level 26) or higher</td></tr>
                            <tr><td>Target SDK</td><td>Android 15 (API Level 35)</td></tr>
                            <tr><td>Architecture</td><td>arm64-v8a, armeabi-v7a, x86_64</td></tr>
                            <tr><td>Package Size</td><td>~45 MB (includes all 1,324+ animations)</td></tr>
                            <tr><td>RAM Requirement</td><td>Minimum 2 GB RAM (3 GB+ recommended)</td></tr>
                            <tr><td>Local Storage</td><td>~85 MB installed (with full exercise cache)</td></tr>
                            <tr><td>Network Permissions</td><td>Zero (INTERNET permission not requested)</td></tr>
                            <tr><td>Display Support</td><td>Optimized for phone displays (360dp to 480dp width)</td></tr>
                        </tbody>
                    </table>

                    <h2 style="margin-top: 10px;">Security & Data Privacy</h2>
                    <p>KumaLift operates under an absolute zero-knowledge privacy model. The application requests zero network permissions, embeds no advertising trackers, and connects to no analytics SDKs. All personal training and bodily measurement records remain exclusively under physical user control.</p>
                </div>

                <div>
                    <h2>Version 1.0.0 Release Notes</h2>
                    <ul class="step-list">
                        <li><strong>Zero-Default Architecture:</strong> Default routine templates and dummy body measurements eliminated; clean initial state.</li>
                        <li><strong>Flexible Load & Reps Keypad:</strong> Unconstrained decimal entry for kilograms and repetitions without 5 kg step limitations.</li>
                        <li><strong>Accurate Workout Stopwatch:</strong> Session timer initiates strictly upon tapping Start Workout, eliminating pre-session time drift.</li>
                        <li><strong>Aligned Logger Table:</strong> Standardized column spacing across Set, Prev, KG, Reps, RPE, and Check cells.</li>
                        <li><strong>Precision Notification Scheduling:</strong> Integrated Android exact alarm scheduling with sub-minute execution tolerance.</li>
                        <li><strong>7-Day Backup Cooldown:</strong> Intelligent cooldown algorithm prevents intrusive repetitive backup reminder modals.</li>
                    </ul>

                    <div style="margin-top: 14px; text-align: center; border-top: 1px solid #1E293B; padding-top: 12px;">
                        <div style="font-weight: 700; color: #F1F5F9; font-size: 9.5pt;">KumaLift • Strength Tracking Perfected</div>
                        <div style="color: #64748B; font-size: 7.5pt; margin-top: 3px;">Published by BearBones Development • Build 1.0.0 (Release)</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Release 1.0.0</span>
            <span>Page 24</span>
        </div>
    </div>
    """)

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>KumaLift User Guide (English Edition)</title>
    <style>
    {css}
    </style>
</head>
<body>
    {''.join(pages)}
</body>
</html>
"""
    with open(OUT_FILE, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"English User Guide HTML saved to: {OUT_FILE} ({len(pages)} pages)")

if __name__ == "__main__":
    build()
