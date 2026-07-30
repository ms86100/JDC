/**
 * Centralized chart / canvas color constants.
 * These mirror the Airbus DS CSS custom properties so that
 * canvas‑based and SVG‑based drawing code stays in sync with the
 * design‑system palette without needing `var(--sa-*)`.
 *
 * If a token value changes in the theme, update the corresponding
 * entry here as well.
 */
export const chartColors = {
  primary: '#255fcc',      // --sa-brand-500
  primaryLight: '#638ee0', // --sa-brand-400
  primaryBg: '#e5ecf7',    // --sa-brand-50
  success: '#08875b',      // --sa-success-500
  successLight: '#e3fcef', // --sa-success-50
  warning: '#ffc929',      // --sa-warn-500
  warningDark: '#ddab17',  // --sa-warn-600
  danger: '#e4002b',       // --sa-danger-500
  dangerLight: '#ffebe6',  // --sa-danger-50
  neutral900: '#282e3a',   // --sa-n900
  neutral700: '#505d74',   // --sa-n700
  neutral600: '#63728a',   // --sa-n600
  neutral400: '#b3bbc8',   // --sa-n400
  neutral200: '#e0e3e9',   // --sa-n200
  neutral100: '#eff1f4',   // --sa-n100
  neutral50: '#fafafa',    // --sa-n50
  white: '#ffffff',        // --sa-n0
  purple: '#6554c0',       // --sa-accent-purple
  teal: '#00a3bf',         // --sa-accent-teal
  orange: '#ff7700',       // --sa-accent-orange
};
