# Praxis Android - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Install the App

```bash
# Connect your Android device via USB
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or scan the QR code from the APK file.

---

### Step 2: Sign In

1. Open **Praxis** app
2. Tap **Sign In with Google** or create account manually
3. Complete onboarding (select your goals)

---

### Step 3: Connect Your Apps

1. Tap **Profile** tab (bottom right)
2. Tap **Settings** ⚙️
3. Tap **Integrations**
4. Tap **Connect** on the apps you use:

#### Option A: Health Connect (Recommended)
- **What:** Automatically logs steps, workouts, sleep, heart rate
- **Setup:** 
  1. Install **Health Connect** app from Play Store (if not installed)
  2. Tap **Connect** in Praxis
  3. Grant permissions when prompted
  4. ✅ Done! Data will sync automatically

#### Option B: Strava
- **What:** Logs running, cycling, hiking activities
- **Setup:**
  1. Tap **Connect Strava**
  2. Authorize in browser
  3. Return to app
  4. ✅ Done!

#### Option C: Google Calendar
- **What:** Logs events and meetings
- **Setup:**
  1. Tap **Connect Google Calendar**
  2. Select your Google account
  3. ✅ Done!

#### Option D: Yazio
- **What:** Logs nutrition and meal data
- **Setup:**
  1. Tap **Connect Yazio**
  2. Authorize in browser
  3. ✅ Done!

#### Option E: Fitbit
- **What:** Logs activity, sleep, heart rate (alternative to Health Connect)
- **Setup:**
  1. Tap **Connect Fitbit**
  2. Authorize in browser
  3. ✅ Done!

---

### Step 4: Add Home Screen Widgets

1. **Long-press** on your home screen
2. Tap **Widgets**
3. Scroll to **Praxis**
4. **Drag** your favorite widget to home screen

#### Available Widgets:

**📊 Praxis Trackers** (Most Popular)
- Shows today's tracker count
- Displays connected integrations
- Tap to open app

**🎯 Goal Progress**
- Visual progress bar
- Quick +5% button

**🔥 Praxis Stats**
- Streak count
- Praxis Points

**💬 Axiom Chat**
- Last coaching message
- Tap to continue

**📅 Daily Quote**
- Motivational guidance
- Fresh content daily

---

### Step 5: Add Quick Settings Tiles (Optional)

1. Swipe **down twice** to open Quick Settings
2. Tap **pencil icon** (Edit)
3. Scroll to **Praxis Sync** and **Quick Log**
4. **Drag** to active tiles

#### Tile Functions:

**🔄 Praxis Sync**
- Tap to sync all connected apps immediately
- Opens app after sync

**➕ Quick Log**
- Tap to open Quick Log dialog
- Manually log activities

---

## 📊 Understanding Your Data

### Tracker Entries

Each integration logs data to specific trackers:

| Integration | Tracker Type | Example Entry |
|-------------|--------------|---------------|
| Health Connect | `steps` | 8,542 steps |
| Health Connect | `cardio` | 45 min workout |
| Health Connect | `sleep` | 7.5 hours |
| Strava | `cardio` | 5k run in 25 min |
| Google Calendar | `journal` | Team meeting (1hr) |
| Yazio | `meal` | 2,100 cal, 150g protein |
| Fitbit | `cardio` | 10,000 steps, 300 cal |
| Fitbit | `sleep` | 8 hours, 92% efficiency |

### Praxis Points

- **Manual entry:** +1 PP (max 3/day)
- **Auto-logged:** +1 PP per entry
- **Goal completion:** +10 PP
- **Daily check-in:** +5 PP

### Widget Display

**Example Widget:**
```
┌──────────────────────┐
│   TRACKERS           │
│                      │
│       7              │
│   ENTRIES TODAY      │
│                      │
│ Auto-sync: Health,   │
│ Strava, Calendar     │
└──────────────────────┘
```

---

## ⚙️ Managing Integrations

### View Connected Apps

1. Settings → Integrations
2. Connected apps show **"Connected"** status
3. Last sync time displayed

### Manual Sync

1. Settings → Integrations
2. Tap **🔄** next to an integration
3. Or tap **Sync All** at bottom

### Disconnect an App

1. Settings → Integrations
2. Tap **🗑️** next to the integration
3. Confirm disconnect

### Reconnect

1. Tap **Connect** on disconnected integration
2. Complete authorization flow
3. Data will sync automatically

---

## 🔔 Notifications & Sync

### Automatic Sync

- **Frequency:** Every 6 hours
- **Requirements:** Network connection
- **Battery Impact:** Minimal (optimized via WorkManager)

### Manual Sync

Use when you want immediate update:
- Quick Settings tile → **Praxis Sync**
- Settings → **Sync All** button
- Individual integration sync button

### What Gets Synced

- Last 24 hours of activity from each integration
- Only new entries (no duplicates)
- Data mapped to appropriate tracker types

---

## ❓ Troubleshooting

### Widget Shows "0 Entries"

**Solutions:**
1. Open app and wait 30 seconds
2. Pull down to refresh (if supported)
3. Trigger manual sync
4. Check if integration is connected

### Integration Not Connecting

**Solutions:**
1. Check internet connection
2. Verify app permissions (Settings → Apps → Praxis)
3. Try disconnecting and reconnecting
4. Check logcat: `adb logcat | grep Praxis`

### Health Connect Not Working

**Solutions:**
1. Install Health Connect app from Play Store
2. Go to Settings → Apps → Praxis → Permissions
3. Grant all Health permissions
4. Restart app

### Strava/Fitbit/Yazio OAuth Fails

**Solutions:**
1. Ensure you're logged into the service in browser
2. Clear browser cache
3. Try different browser
4. Check if service is available in your region

### Quick Settings Tile Not Appearing

**Solutions:**
1. Swipe down twice fully
2. Tap Edit (pencil icon)
3. Scroll to find Praxis tiles
4. Drag to active tiles

---

## 🎯 Pro Tips

### Maximize Praxis Points

1. **Connect multiple integrations** - More sources = more entries
2. **Manual check-ins** - +5 PP daily
3. **Complete goals** - +10 PP per completion
4. **Engage with matches** - Message accountability partners

### Best Widget Placement

1. **Home screen center** - Easy visibility
2. **Lock screen** (if supported) - Quick glance
3. **Combine with other productivity widgets** - Create command center

### Optimize Sync

1. **Sync before opening app** - See latest data
2. **Use Quick Settings tile** - Fastest method
3. **Sync after workouts** - Log activity immediately
4. **Check widget after sync** - Verify entries logged

### Privacy Control

1. **Revoke permissions anytime** - Settings → Apps → Praxis
2. **Disconnect integrations** - Settings → Integrations → Disconnect
3. **Delete auto-logged entries** - Open app → Diary → Delete entry
4. **Pause sync** - Disable integration temporarily

---

## 📱 Next Steps

### Explore the App

- **Dashboard** - View matches, achievements, stats
- **Goals** - Track progress, update milestones
- **Matches** - Find accountability partners
- **Diary** - See all your tracker entries in feed
- **Analytics** - View trends and insights

### Invite Friends

1. Profile → Referrals
2. Share your invite code
3. Both get bonus Praxis Points

### Upgrade to Premium

- Unlimited goals (free: 3)
- Advanced analytics
- Priority matching
- AI coaching tips
- **$9.99/month**

---

## 🆘 Need Help?

### Quick Diagnostics

```bash
# Check if app is installed
adb shell pm list packages | grep praxis

# View logs
adb logcat | grep -E "Praxis|Integration"

# Force sync via adb
adb shell cmd jobscheduler run -f com.praxis.app 1

# Check workmanager status
adb shell dumpsys jobscheduler | grep -i praxis
```

### Common Log Messages

**✅ Success:**
```
D/HealthConnectService: Health Connect sync complete: 3 entries logged
D/StravaService: Strava sync complete: 2 entries logged
D/IntegrationManager: Logged tracker entry: cardio from auto_strava
```

**⚠️ Warning:**
```
W/HealthConnectService: Health Connect permissions not granted
W/StravaService: No Strava token - need to run OAuth flow
```

**❌ Error:**
```
E/IntegrationManager: Failed to log tracker entry
E/StravaService: Token refresh failed
```

### Contact Support

- **Email:** support@praxis.app
- **Discord:** Join Praxis community server
- **GitHub:** Open issue with logs attached

---

## 🎉 You're All Set!

Your Praxis app is now connected to your favorite apps. Here's what will happen:

✅ **Every 6 hours** - Auto-sync runs in background  
✅ **New activities** - Automatically logged to trackers  
✅ **Widget updates** - Shows latest tracker count  
✅ **Praxis Points** - Earned for each entry  
✅ **Quick access** - Via Quick Settings tile  

**Start living intentionally. Track everything. Achieve more. 🚀**

---

**Built with ❤️ for the Praxis community**
