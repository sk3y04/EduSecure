/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['var(--font-body)', 'Segoe UI', 'sans-serif'],
        body: ['var(--font-body)', 'Segoe UI', 'sans-serif'],
        display: ['var(--font-display)', 'Segoe UI', 'sans-serif'],
      },
      colors: {
        canvas: 'var(--color-bg)',
        surface: 'var(--color-surface)',
        'surface-2': 'var(--color-surface-2)',
        border: 'var(--color-border)',
        text: 'var(--color-text)',
        heading: 'var(--color-heading)',
        primary: 'var(--color-primary)',
        success: 'var(--color-success)',
        warning: 'var(--color-warning)',
        danger: 'var(--color-danger)',
      },
      boxShadow: {
        panel: 'var(--shadow-soft)',
        elevated: 'var(--shadow-hover)',
      },
    },
  },
  plugins: [],
}
