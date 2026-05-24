import React from 'react';

interface KeyboardShortcutsModalProps {
  onClose: () => void;
}

const shortcuts = [
  {
    section: 'Navigation',
    items: [
      { keys: ['←', '→'], desc: 'Move focus between columns' },
      { keys: ['↑', '↓'], desc: 'Move focus between cards' },
      { keys: ['Tab', 'Shift+Tab'], desc: 'Navigate to next/previous column' },
      { keys: ['Home', 'End'], desc: 'Go to first/last card in column' },
    ],
  },
  {
    section: 'Issue Actions',
    items: [
      { keys: ['Enter'], desc: 'Open selected issue' },
      { keys: ['E'], desc: 'Edit selected issue' },
      { keys: ['A'], desc: 'Assign to me' },
      { keys: ['M'], desc: 'Move issue' },
      { keys: ['C'], desc: 'Copy issue link' },
      { keys: ['Del'], desc: 'Delete issue' },
    ],
  },
  {
    section: 'Drag & Drop',
    items: [
      { keys: ['Space'], desc: 'Start/stop drag on selected issue' },
      { keys: ['Shift+Enter'], desc: 'Start drag on selected issue' },
      { keys: ['Esc'], desc: 'Cancel drag operation' },
      { keys: ['←', '→', '↑', '↓'], desc: 'Move dragged issue' },
    ],
  },
  {
    section: 'Filtering',
    items: [
      { keys: ['F'], desc: 'Focus search filter' },
      { keys: ['Q'], desc: 'Toggle "My Issues" filter' },
      { keys: ['R'], desc: 'Refresh board' },
    ],
  },
  {
    section: 'General',
    items: [
      { keys: ['?'], desc: 'Show this help' },
      { keys: ['Esc'], desc: 'Close dialogs/panels' },
      { keys: ['Ctrl', '+', 'K'], desc: 'Command palette' },
    ],
  },
];

export default function KeyboardShortcutsModal({ onClose }: KeyboardShortcutsModalProps) {
  return (
    <div className="sa-keyboard-shortcuts-overlay" onClick={onClose}>
      <div
        className="sa-keyboard-shortcuts-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="keyboard-shortcuts-title"
      >
        <div className="sa-keyboard-shortcuts-header">
          <h2 id="keyboard-shortcuts-title" className="sa-keyboard-shortcuts-title">
            Keyboard Shortcuts
          </h2>
          <button
            type="button"
            className="sa-keyboard-shortcuts-close"
            onClick={onClose}
            aria-label="Close"
          >
            ×
          </button>
        </div>
        <div className="sa-keyboard-shortcuts-body">
          {shortcuts.map((group) => (
            <div key={group.section} className="sa-keyboard-shortcuts-section">
              <h3 className="sa-keyboard-shortcuts-section-title">{group.section}</h3>
              <table className="sa-shortcuts-table">
                <tbody>
                  {group.items.map((item, idx) => (
                    <tr key={idx}>
                      <td>
                        <span className="sa-shortcut-keys">
                          {item.keys.map((key, keyIdx) => (
                            <React.Fragment key={keyIdx}>
                              {keyIdx > 0 && ' + '}
                              <kbd className="sa-shortcut-key">{key}</kbd>
                            </React.Fragment>
                          ))}
                        </span>
                      </td>
                      <td className="sa-shortcut-desc">{item.desc}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
