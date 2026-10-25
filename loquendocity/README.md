# Daisy

Teclado de rueda controlado con un mando, con salida de texto a voz.

Mueves el stick izquierdo para elegir un pétalo de la rueda y pulsas uno de los cuatro botones (Y, X, B, A) para escribir una letra. Cuando terminas, el texto se pasa a voz con la voz instalada que elijas, por ejemplo las voces de Loquendo.

## Cómo se usa

La rueda tiene 8 pétalos, con 4 caracteres cada uno. El stick izquierdo selecciona el pétalo y el botón elige el carácter dentro de él:

| Botón | Posición en el pétalo |
|-------|-----------------------|
| Y     | arriba                |
| X     | izquierda             |
| B     | derecha               |
| A     | abajo                 |

Los pétalos van en sentido horario, empezando por el de arriba:

| Pétalo | Y (arriba) | X (izq.) | B (der.) | A (abajo) |
|--------|-----------|----------|----------|-----------|
| 1      | b         | a        | c        | d         |
| 2      | f         | e        | g        | h         |
| 3      | j         | i        | k        | l         |
| 4      | n         | m        | o        | p         |
| 5      | r         | q        | s        | t         |
| 6      | v         | u        | w        | x         |
| 7      | z         | y        | ,        | espacio   |
| 8      | mensaje 00 | mensaje 01 | mensaje 11 | mensaje 10 |

El pétalo 8 (el del contador binario) escribe mensajes personalizados, que se editan en los cuatro campos de texto de la ventana.

### Controles extra

| Control | Acción |
|---------|--------|
| L3 (clic del stick izquierdo) | Habla el texto y lo borra para escribir otro |
| LB | Borra el último carácter |
| RB | Borra todo el texto |

## Voz

La app lista las voces SAPI5 instaladas en Windows. Si tienes voces de Loquendo instaladas, aparecen en el desplegable junto con las demás. También se puede ajustar la velocidad.

Las voces de Loquendo suelen ser de 32 bits, así que la app consulta tanto el PowerShell de 64 bits como el de 32 bits (`SysWOW64`). Loquendo es software comercial y no se incluye en este proyecto.

## Requisitos

- Windows (la voz usa `System.Speech`)
- JDK 17 o superior
- Un mando compatible, como uno de Xbox
- Voces SAPI5 instaladas (opcional si solo quieres probar la rueda)

## Ejecutar

El proyecto usa Gradle con un subproyecto `app`. Desde la raíz:

```powershell
.\gradlew.bat :app:run
```

O ábrelo en Apache NetBeans con **File → Open Project** y usa **Run Project**.

Para generar un jar con todo incluido:

```powershell
.\gradlew.bat :app:fatJar
```

El resultado queda en `app/build/libs/`.

## Estructura

```
loquendocity/
├── settings.gradle
└── app/
    ├── build.gradle
    └── src/main/java/loquendocity/loquendocity.java
```

La clase principal es `loquendocity.loquendocity`.

## Dependencias

- [Jamepad](https://github.com/libgdx/Jamepad) `2.0.14.0` para leer el mando
- `gdx-jnigen-loader` `2.5.2`, declarado a mano porque Jamepad apunta a una versión de desarrollo que no está publicada

## Problemas conocidos

- **El stick va al revés.** Si arriba y abajo salen invertidos, cambia `leftStickY` por `-leftStickY` en el código.
- **Error al borrar `build`.** Si el proyecto está dentro de OneDrive, Gradle puede fallar con "Unable to delete directory". Mueve el proyecto fuera de OneDrive, por ejemplo a `C:\dev`.
- **No aparecen las voces.** Revisa que estén instaladas y registradas en Windows. Si son de 32 bits, deben estar registradas para SAPI5.
