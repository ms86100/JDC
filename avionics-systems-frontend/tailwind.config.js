/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        airbus: {
          blue: '#255fcc',
          'blue-dark': '#063b9e',
          navy: '#00205b',
          gray: '#505d74',
          'gray-light': '#63728a',
          border: '#e0e3e9',
          surface: '#eff1f4',
        },
        jira: {
          blue: '#255fcc',
          'blue-dark': '#063b9e',
          navy: '#00205b',
          gray: '#505d74',
          'gray-light': '#63728a',
          border: '#e0e3e9',
          surface: '#eff1f4',
        },
      },
      fontFamily: {
        jira: [
          'Inter',
          'Arial',
          'sans-serif',
        ],
        sans: [
          'Inter',
          'Arial',
          'sans-serif',
        ],
        mono: [
          'Roboto Mono',
          'monospace',
        ],
      },
    },
  },
  plugins: [],
};
