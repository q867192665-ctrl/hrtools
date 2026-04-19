# Design System: Enterprise Blue

## 1. Visual Theme & Atmosphere

### Design Philosophy
- Professional enterprise management system aesthetic
- Clean, trustworthy, and efficient
- Light-first design with clear visual hierarchy
- High contrast for excellent readability
- Content density: Medium (information-rich but organized)

### Mood Keywords
- Professional
- Trustworthy
- Clean
- Efficient
- Corporate
- Modern

### Visual Identity
- Classic blue and white color scheme
- Clear section divisions
- Subtle shadows for depth
- Focus on productivity and workflow

---

## 2. Color Palette & Roles

### Primary Colors
| Token | Hex | RGB | Usage |
|-------|-----|-----|-------|
| `--bg-primary` | #F5F7FA | rgb(245, 247, 250) | Main background |
| `--bg-secondary` | #FFFFFF | rgb(255, 255, 255) | Card backgrounds, panels |
| `--bg-tertiary` | #EEF2F7 | rgb(238, 242, 247) | Hover states, table stripes |
| `--bg-quaternary` | #E4E9F0 | rgb(228, 233, 240) | Borders, dividers |

### Accent Colors (Enterprise Blue)
| Token | Hex | RGB | Usage |
|-------|-----|-----|-------|
| `--accent-primary` | #1E88E5 | rgb(30, 136, 229) | Primary buttons, links, active states |
| `--accent-hover` | #1976D2 | rgb(25, 118, 210) | Button hover |
| `--accent-light` | #E3F2FD | rgb(227, 242, 253) | Light backgrounds, badges |
| `--accent-subtle` | #BBDEFB | rgb(187, 222, 251) | Subtle accents |

### Text Colors
| Token | Hex | RGB | Usage |
|-------|-----|-----|-------|
| `--text-primary` | #212121 | rgb(33, 33, 33) | Headings, primary text |
| `--text-secondary` | #616161 | rgb(97, 97, 97) | Body text, descriptions |
| `--text-tertiary` | #9E9E9E | rgb(158, 158, 158) | Placeholder, disabled |
| `--text-muted` | #BDBDBD | rgb(189, 189, 189) | Subtle labels |

### Semantic Colors
| Token | Hex | RGB | Usage |
|-------|-----|-----|-------|
| `--success` | #4CAF50 | rgb(76, 175, 80) | Success states |
| `--warning` | #FF9800 | rgb(255, 152, 0) | Warnings, alerts |
| `--error` | #F44336 | rgb(244, 67, 54) | Errors, destructive actions |
| `--info` | #2196F3 | rgb(33, 150, 243) | Information |

### Border Colors
| Token | Hex | RGB | Usage |
|-------|-----|-----|-------|
| `--border-primary` | #E0E0E0 | rgb(224, 224, 224) | Primary borders |
| `--border-secondary` | #EEEEEE | rgb(238, 238, 238) | Subtle dividers |
| `--border-accent` | #1E88E5 | rgb(30, 136, 229) | Focus rings |

---

## 3. Typography Rules

### Font Family
- **Primary**: "Microsoft YaHei", "PingFang SC", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif
- **Monospace**: Consolas, "Courier New", monospace (for code/IDs)

### Type Scale
| Level | Size | Weight | Line Height | Letter Spacing | Usage |
|-------|------|--------|-------------|----------------|-------|
| H1 | 28px | 600 | 1.3 | -0.01em | Page titles |
| H2 | 22px | 600 | 1.4 | -0.01em | Section headers |
| H3 | 18px | 600 | 1.4 | 0 | Card titles |
| H4 | 16px | 600 | 1.5 | 0 | Subsection headers |
| Body | 14px | 400 | 1.6 | 0 | Body text |
| Body Small | 13px | 400 | 1.5 | 0 | Secondary text |
| Caption | 12px | 500 | 1.4 | 0.01em | Labels, badges |

---

## 4. Component Stylings

### Buttons

#### Primary Button
```css
background: #1E88E5;
color: #FFFFFF;
border: none;
border-radius: 4px;
padding: 8px 20px;
font-size: 14px;
font-weight: 500;
transition: all 0.2s ease;

&:hover {
  background: #1976D2;
  box-shadow: 0 2px 8px rgba(30, 136, 229, 0.3);
}

&:active {
  background: #1565C0;
}
```

#### Secondary Button
```css
background: #FFFFFF;
color: #616161;
border: 1px solid #E0E0E0;
border-radius: 4px;
padding: 8px 20px;
font-size: 14px;
font-weight: 500;

&:hover {
  background: #F5F7FA;
  border-color: #BDBDBD;
  color: #212121;
}
```

#### Danger Button
```css
background: #F44336;
color: #FFFFFF;
border: none;
border-radius: 4px;
padding: 8px 20px;

&:hover {
  background: #E53935;
}
```

### Cards
```css
background: #FFFFFF;
border: 1px solid #E0E0E0;
border-radius: 8px;
padding: 20px;
box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
transition: box-shadow 0.2s ease;

&:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
```

### Inputs
```css
background: #FFFFFF;
border: 1px solid #E0E0E0;
border-radius: 4px;
padding: 10px 14px;
color: #212121;
font-size: 14px;
transition: all 0.2s ease;

&:focus {
  border-color: #1E88E5;
  box-shadow: 0 0 0 3px rgba(30, 136, 229, 0.1);
  outline: none;
}

&::placeholder {
  color: #9E9E9E;
}
```

### Tables
```css
table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: #F5F7FA;
  color: #616161;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 16px;
  text-align: left;
  border-bottom: 2px solid #E0E0E0;
}

td {
  padding: 14px 16px;
  border-bottom: 1px solid #EEEEEE;
  font-size: 14px;
  color: #212121;
}

tr:hover td {
  background: #F5F7FA;
}
```

### Badges
```css
/* Status Badge - Default */
background: #E3F2FD;
color: #1976D2;
border-radius: 4px;
padding: 4px 10px;
font-size: 12px;
font-weight: 500;

/* Status Badge - Success */
background: #E8F5E9;
color: #388E3C;

/* Status Badge - Warning */
background: #FFF3E0;
color: #F57C00;

/* Status Badge - Error */
background: #FFEBEE;
color: #D32F2F;
```

### Modals
```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal-content {
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  max-width: 500px;
  width: 90%;
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #EEEEEE;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #EEEEEE;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
```

---

## 5. Layout Principles

### Spacing Scale
| Token | Value | Usage |
|-------|-------|-------|
| `--space-1` | 4px | Tight gaps |
| `--space-2` | 8px | Compact spacing |
| `--space-3` | 12px | Default element spacing |
| `--space-4` | 16px | Card padding, section gaps |
| `--space-5` | 20px | Large gaps |
| `--space-6` | 24px | Section margins |
| `--space-8` | 32px | Major sections |

### Layout Patterns
- Header with navigation
- Sidebar for admin functions
- Card-based content areas
- Clean table layouts
- Modal dialogs for details

---

## 6. Header & Navigation

### Header
```css
.header {
  background: #FFFFFF;
  border-bottom: 1px solid #E0E0E0;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #212121;
  display: flex;
  align-items: center;
  gap: 10px;
}
```

### Navigation Tabs
```css
.nav-tabs {
  display: flex;
  gap: 4px;
  background: #F5F7FA;
  padding: 4px;
  border-radius: 8px;
}

.nav-tab {
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #616161;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-tab:hover {
  color: #212121;
  background: #FFFFFF;
}

.nav-tab.active {
  background: #FFFFFF;
  color: #1E88E5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
```

---

## 7. CSS Variables

```css
:root {
  /* Backgrounds */
  --bg-primary: #F5F7FA;
  --bg-secondary: #FFFFFF;
  --bg-tertiary: #EEF2F7;
  --bg-quaternary: #E4E9F0;
  
  /* Accents */
  --accent-primary: #1E88E5;
  --accent-hover: #1976D2;
  --accent-light: #E3F2FD;
  
  /* Text */
  --text-primary: #212121;
  --text-secondary: #616161;
  --text-tertiary: #9E9E9E;
  --text-muted: #BDBDBD;
  
  /* Semantic */
  --success: #4CAF50;
  --warning: #FF9800;
  --error: #F44336;
  --info: #2196F3;
  
  /* Borders */
  --border-primary: #E0E0E0;
  --border-secondary: #EEEEEE;
  
  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
}
```

---

## 8. Responsive Behavior

### Breakpoints
| Breakpoint | Width | Behavior |
|------------|-------|----------|
| Mobile | < 640px | Single column, stacked layout |
| Tablet | 640px - 1024px | 2-column grid |
| Desktop | > 1024px | Full layout |

### Touch Targets
- Minimum: 44px × 44px
- Button height: 36px-40px
