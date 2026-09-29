import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import '../ui/theme/tokens.css';
import '../ui/auth/auth.css';
import '../ui/shell/shell.css';
import '../ui/common/common.css';
import '../ui/period/period.css';
import '../ui/home/home.css';
import '../ui/movements/movements.css';
import '../ui/styles.css';

import { App } from './app';
import { createContainer } from './composition-root';

const root = document.getElementById('root');
if (root === null) throw new Error('No existe el elemento #root en index.html');

const container = createContainer(import.meta.env);

createRoot(root).render(
  <StrictMode>
    <App container={container} />
  </StrictMode>,
);
