import React from 'react';
import { createRoot } from 'react-dom/client';

function App() {
  return <main><h1>AKIIP</h1><p>AI Knowledge and Incident Intelligence Platform</p></main>;
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);
