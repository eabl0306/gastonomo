# ADR 0006: Estrategia de testing y umbrales de cobertura

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

"Todo tan testeado como sea posible" no es una instruccion ejecutable. Hace falta decidir
que se testea en cada nivel, con que herramienta y con que umbral, y que ese umbral
rompa el build cuando baja.

La arquitectura hexagonal hace esto mas facil: el nucleo no tiene dependencias, asi que
se testea sin mocks de framework, y los adaptadores se testean contra dobles o contra la
cosa real, segun cueste.

## Decision

Tres niveles en las tres aplicaciones:

1. Unitarios sobre el nucleo. Sin mocks de framework: los puertos se implementan con
   fakes escritos a mano (`FakeHealthProbe`, `InMemoryHealthGateway`, `FakeHealthGateway`).
   Aqui va el 80% del esfuerzo.
2. De adaptador. El adaptador de PostgreSQL contra un PostgreSQL de verdad en Docker. El
   adaptador HTTP de la web contra `fetch` interceptado. El de Android contra el
   `MockEngine` de Ktor. Los handlers HTTP con `salvo::test::TestClient`.
3. De punta a punta. Playwright contra el preview de la web con la red mockeada, y el
   test instrumentado de Android con Compose sobre un emulador. Ademas, `just e2e-stack`
   levanta la pila de Docker entera y corre Playwright contra ella sin mockear nada:
   navegador, web, API y PostgreSQL. Es el unico test que prueba el contrato real entre
   la web y la API.

Ademas hay tests de arquitectura, que son tests de verdad y fallan como los demas: uno
lee `Cargo.lock` y prohibe infraestructura en el nucleo, y ocho reglas de Konsist vigilan
la direccion de dependencias en Android.

Umbrales, verificados en CI:

| Ambito                    | Minimo | Real    |
| ------------------------- | ------ | ------- |
| Nucleo de la API          | 80%    | 99,56%  |
| Global de la API          | 60%    | 85,95%  |
| Dominio y aplicacion web  | 80%    | 100%    |
| Global web                | 60%    | 98,8%   |
| Modulos de nucleo Android | 80%    | 87-100% |
| Agregado Android          | 60%    | 95,07%  |

Herramientas: `cargo-llvm-cov`, la cobertura v8 de vitest con umbrales por glob, y Kover
con un umbral por modulo.

Se prohibe apagar un test sin dejar escrito por que: `.only`, `#[ignore]` pelado y
`@Ignore` sin motivo rompen el build.

## Consecuencias

A favor:

- Un cambio que rompe una regla de negocio falla en un test unitario de milisegundos, no
  en un e2e de dos minutos.
- La cobertura no es decorativa: bajar del umbral rompe la CI.

En contra:

- La cobertura global de la API no incluye el crate binario: `cargo-llvm-cov` no genera
  datos para un binario que ningun test enlaza. El composition root se verifica a mano.
  Para cubrirlo habria que partirlo en lib mas un main fino.
- Los tests instrumentados de Android no corren en CI por defecto: necesitan emulador.
  Estan listos y se pueden activar con `reactivecircus/android-emulator-runner`.
- Los e2e que corre la CI mockean la red. Eso los hace rapidos y deterministas, pero no
  prueban el contrato real contra la API. Eso lo cubre `just e2e-stack`, que no esta en
  la CI porque levantar la pila entera cuesta varios minutos: se corre a mano antes de
  tocar el contrato compartido.
