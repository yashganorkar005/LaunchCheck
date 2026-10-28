# ReactBits TechText source bundle

These files were extracted from the JavaScript + CSS TechText source the user supplied from the official ReactBits page. The component code is preserved; `TechText.tsx` adds `// @ts-nocheck` because the supplied upstream source is JavaScript and has no TypeScript prop definitions.

- `TechText.jsx`: plain upstream JavaScript/JSX source.
- `TechText.tsx`: convenient JSX-in-TSX option for the LaunchCheck TypeScript/Vite project; it is not a fully typed rewrite.
- `TechText.css`: required component CSS. Keep this beside the component because it is imported as `./TechText.css`.

For LaunchCheck, place either the `.tsx` or `.jsx` component and `TechText.css` together in a suitable frontend source directory, then import it into the actual landing-page hero headline. Give the wrapper a non-zero responsive height (for example `clamp(130px, 18vw, 200px)`). Ensure the hero renders the component and visually inspect the canvas. Do not install or render FluxLines as part of this task.

Official page: https://reactbits.dev/text-animations/tech-text
