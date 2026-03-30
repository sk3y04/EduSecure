/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef4ff',
          100: '#d9e7ff',
          200: '#b9d0ff',
          500: '#446fbe',
          600: '#355b9e',
          700: '#28497f',
          800: '#203a67',
        },
      },
      boxShadow: {
        panel: '0 14px 30px -24px rgba(15, 23, 42, 0.4)',
      },
    },
  },
  plugins: [],
}
