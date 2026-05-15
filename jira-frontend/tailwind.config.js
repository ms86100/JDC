/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        jira: {
          blue: '#0052cc',
          'blue-dark': '#0043a6',
          navy: '#172b4d',
          gray: '#42526e',
          'gray-light': '#6b778c',
          border: '#dfe1e6',
          surface: '#f4f5f7',
        },
      },
      fontFamily: {
        jira: [
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'Helvetica Neue',
          'Arial',
          'sans-serif',
        ],
      },
    },
  },
  plugins: [],
};
