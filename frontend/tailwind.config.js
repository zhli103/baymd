/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: '#F4F5F7',
        panel: '#E8EBEF',
        'text-primary': '#1A1D28',
        accent: '#5C3D4E',
        'accent-light': '#F0E8EC',
        vital: '#E05555',
        muted: '#8899A6',
        border: '#DDE1E5',
      },
      fontFamily: {
        display: ['Lora', 'serif'],
        body: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      animation: {
        ekg: 'ekg-wave 1.6s ease-in-out infinite',
        'fade-in': 'fade-in 0.4s ease-out',
      },
      keyframes: {
        'ekg-wave': {
          '0%': { transform: 'scaleX(0.3)', opacity: '0.4' },
          '30%': { transform: 'scaleX(1.2)', opacity: '1' },
          '50%': { transform: 'scaleX(0.6)', opacity: '0.7' },
          '70%': { transform: 'scaleX(1.0)', opacity: '0.9' },
          '100%': { transform: 'scaleX(0.3)', opacity: '0.4' },
        },
        'fade-in': {
          from: { opacity: '0', transform: 'translateY(4px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}
