const apiUrl = (import.meta.env.VITE_API_URL || 'http://127.0.0.1:8081').replace(/\/$/, '')

export async function solicitar(ruta, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${apiUrl}${ruta}`, opciones)
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('No se pudo conectar con Spring Boot. Revisa el backend en el puerto 8081.')
  }
  if (!respuesta.ok) {
    const texto = await respuesta.text()
    let detalle
    try { detalle = JSON.parse(texto).message } catch { /* Respuestas de versiones anteriores. */ }
    throw new Error(detalle || (respuesta.status === 404 ? 'El producto ya no existe.' : `Error HTTP ${respuesta.status}`))
  }
  return respuesta.status === 204 ? null : respuesta.json()
}

export const rutaProductos = (database) => `/api/${database}/productos`
