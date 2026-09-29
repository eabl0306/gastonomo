import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import '../ui/theme/tokens.css';
import '../ui/auth/auth.css';
import '../ui/shell/shell.css';
import '../ui/styles.css';

import { App } from './app';
import { createContainer } from './composition-root';

const root = document.getElementById('root');
if (root === null) throw new Error('No existe el elemento #root en index.html');

const container = createContainer(import.meta.env);

createRoot(root).render(
  <StrictMode>
    <App
      auth={container.auth}
      health={container.health}
      passwordResets={container.passwordResets}
      session={container.session}
    />
  </StrictMode>,
);
