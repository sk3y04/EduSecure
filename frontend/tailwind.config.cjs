/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f4f7ff',
          100: '#e8efff',
          500: '#4f7cff',
          600: '#335df4',
          700: '#2646cf',
        },
      },
      boxShadow: {
        panel: '0 20px 45px -25px rgba(15, 23, 42, 0.55)',
      },
    },
  },
  plugins: [],
}

