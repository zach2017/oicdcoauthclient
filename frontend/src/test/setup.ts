import '@testing-library/jest-dom/vitest';

// Some components read/modify window.history
Object.defineProperty(window, 'history', {
  value: window.history,
  writable: true,
});
