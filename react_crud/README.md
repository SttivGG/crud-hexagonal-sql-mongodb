# Frontend del CRUD SQL / NoSQL

React 19, Vite 6 y CSS. Consume el backend de [../Spring_boot_crud](../Spring_boot_crud) en **http://127.0.0.1:8081**.

## Ejecutar

Utiliza Node.js 22.13+ o 24 LTS. Desde esta carpeta:

```powershell
npm ci
npm run dev
```

Abre **http://127.0.0.1:5175**. MySQL y MongoDB deben estar disponibles para utilizar sus respectivas opciones.

El selector MySQL/MongoDB cambia la ruta real del CRUD. El formulario y la tabla se reutilizan. MongoDB añade búsqueda por nombre, rango de precios, productos más costosos y estadísticas; cada consulta muestra resultados y el pipeline devuelto por el backend, con opción de copia.

Cambiar de base reinicia la sesión y cancela peticiones anteriores. Las mutaciones invalidan los resultados de filtros. Incluye estados de carga, mensajes, validación, confirmación de borrado, estados vacíos y diseño adaptable.

Para cambiar el backend, copia `.env.example` a `.env.local`, ajusta `VITE_API_URL` y reinicia Vite. Si cambias el origen del frontend, configura también `CORS_ALLOWED_ORIGINS` en el backend.

## Verificar y compilar

```powershell
npm test
npm run build
```

Vitest, Testing Library y jsdom comprueban el CRUD en ambas rutas, respuestas tardías al cambiar de tecnología/filtro, errores, validación de rangos y copia exacta del pipeline. Son dependencias de desarrollo. La compilación queda en `dist/`.

La arquitectura, configuración de bases, endpoints, variables y limitaciones están documentados en el [README principal](../README.md).
