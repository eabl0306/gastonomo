# ADR 0002: Arquitectura hexagonal en los tres apps

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

Las tres aplicaciones van a crecer. La forma habitual de que un proyecto se vuelva
imposible de cambiar es que la logica de negocio acabe mezclada con el framework de turno:
reglas dentro de un handler HTTP, validaciones dentro de un componente de React, decisiones
de negocio dentro de un ViewModel. Cuando eso pasa, testear obliga a levantar el framework
entero y cambiar de framework obliga a reescribir el negocio.

## Decision

Arquitectura hexagonal (puertos y adaptadores) en las tres, con la misma nomenclatura:

- Nucleo: dominio mas casos de uso. No conoce HTTP, ni SQL, ni React, ni Android.
- Puertos: interfaces que declara el nucleo para hablar con el exterior.
- Adaptadores driving: quien invoca los casos de uso (handler, componente, ViewModel).
- Adaptadores driven: quien implementa los puertos (PostgreSQL, cliente HTTP, fakes).
- Composition root: el unico sitio que conoce a todos y cablea el grafo.

La direccion de dependencias se fuerza estructuralmente, no por convencion:

| App     | Separacion                                    | Herramienta que lo verifica                     |
| ------- | --------------------------------------------- | ----------------------------------------------- |
| API     | un crate por capa                             | test que lee `Cargo.lock` y prohibe infra       |
| Android | un modulo por capa, nucleo en `kotlin("jvm")` | el classpath, mas ocho reglas de Konsist        |
| Web     | una carpeta por capa                          | `eslint-plugin-boundaries` con deny por defecto |

En Rust y en Kotlin la barrera la pone el compilador: el dominio no puede ni nombrar a
Salvo ni a `android.util.Log`. En TypeScript no existe esa barrera, asi que la pone el
linter, y se comprobo en caliente que salta.

## Consecuencias

A favor:

- El nucleo se testea sin levantar nada: 99,56% de cobertura de lineas en el de la API y
  100% en el dominio de Android sin un solo mock de framework.
- Cambiar de adaptador es cambiar una linea en el composition root.
- La conversacion de code review deja de ser "esto deberia ir en otro sitio", porque la
  herramienta lo dice antes.

En contra:

- Mas ficheros y mas indireccion para lo mismo. Para el health check hay un DTO, un
  modelo de dominio y un mapeador entre ambos, que a simple vista parece duplicado.
- Hay que resistir la tentacion de pasar el DTO hacia dentro. Cuando el dominio real
  aparezca, esa disciplina es la que da el beneficio.
