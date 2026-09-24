/**
 * Regla local: un marcador de trabajo pendiente solo vale si apunta a un ticket.
 *
 * Las palabras clave se construyen por concatenacion a proposito, para que este
 * fichero no dispare su propia regla ni el script tools/checks/todo-without-ticket.mjs.
 * La comparacion es sensible a mayusculas: en castellano "todo" es una palabra normal.
 */
const WORDS = ['TO' + 'DO', 'FIX' + 'ME', 'HA' + 'CK'];
const MARKER = new RegExp(`\\b(${WORDS.join('|')})\\b`);
const TICKETED = new RegExp(`\\b(${WORDS.join('|')})\\([A-Z][A-Z0-9]*-\\d+\\)`);

/** @type {import('eslint').Rule.RuleModule} */
export const ticketedTodo = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Exige una referencia a ticket en los marcadores de trabajo pendiente.',
    },
    schema: [],
    messages: {
      missingTicket:
        'Marcador de trabajo pendiente sin ticket. Usa el formato {{example}} o borralo.',
    },
  },
  create(context) {
    const example = `${WORDS[0]}(ABC-123): ...`;
    return {
      Program() {
        for (const comment of context.sourceCode.getAllComments()) {
          if (!MARKER.test(comment.value)) continue;
          if (TICKETED.test(comment.value)) continue;
          context.report({ loc: comment.loc, messageId: 'missingTicket', data: { example } });
        }
      },
    };
  },
};

/** Plugin minimo del monorepo, solo para reglas que no existen upstream. */
export const gastonomoPlugin = {
  meta: { name: '@gastonomo/eslint-config', version: '0.1.0' },
  rules: { 'ticketed-todo': ticketedTodo },
};
