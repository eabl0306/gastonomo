import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import '../ui/styles.css';

import { App } from './app';
import { createHealthGateway } from './composition-root';

const container = document.getElementById('root');
if (container === null) throw new Error('No existe el elemento #root en index.html');

createRoot(container).render(
  <StrictMode>
    <App gateway={createHealthGateway(import.meta.env)} />
  </StrictMode>,
);
