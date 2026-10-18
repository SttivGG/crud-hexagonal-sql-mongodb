import { useEffect, useRef, useState } from 'react'
import { solicitar } from '../services/api.js'
import ProductTable from './ProductTable.jsx'

const consultas = [
  ['nombre', 'Buscar por nombre'], ['precios', 'Rango de precios'],
  ['costosos', 'Más costosos'], ['estadisticas', 'Estadísticas generales']
]

export default function MongoFilters() {
  const [tipo, setTipo] = useState('nombre')
  const [campos, setCampos] = useState({ texto: '', minimo: '', maximo: '', limite: '5' })
  const [resultado, setResultado] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')
  const [copiado, setCopiado] = useState('')
  const peticion = useRef(null)
  useEffect(() => () => peticion.current?.abort(), [])

  function cambiarTipo(evento) {
    peticion.current?.abort()
    setTipo(evento.target.value)
    setResultado(null)
    setError('')
    setCopiado('')
    setCargando(false)
  }

  function cambiar(evento) { setCampos({ ...campos, [evento.target.name]: evento.target.value }) }

  async function ejecutar(evento) {
    evento.preventDefault()
    peticion.current?.abort()
    const controller = new AbortController()
    peticion.current = controller
    setError('')
    setCopiado('')
    setResultado(null)
    if (tipo === 'precios' && Number(campos.minimo) > Number(campos.maximo)) {
      setError('El precio mínimo no puede superar el máximo.')
      return
    }
    const parametros = tipo === 'nombre' ? { texto: campos.texto.trim() }
      : tipo === 'precios' ? { minimo: campos.minimo, maximo: campos.maximo }
        : tipo === 'costosos' ? { limite: campos.limite } : {}
    setCargando(true)
    try {
      const datos = await solicitar(`/api/mongodb/productos/filtros/${tipo}?${new URLSearchParams(parametros)}`, { signal: controller.signal })
      if (!controller.signal.aborted) setResultado(datos)
    } catch (fallo) {
      if (!controller.signal.aborted) setError(fallo.message)
    } finally {
      if (!controller.signal.aborted) setCargando(false)
    }
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(JSON.stringify(resultado.pipeline, null, 2))
      setCopiado('Pipeline copiado.')
    } catch { setCopiado('No se pudo copiar. Puedes seleccionar y copiar el código manualmente.') }
  }

  return (
    <section className="tarjeta filtros" aria-labelledby="titulo-filtros">
      <p className="etiqueta">Sección de agregación MongoDB</p>
      <h2 id="titulo-filtros">Explorar productos</h2>
      <p className="ayuda">Ejecuta una consulta y revisa el pipeline utilizado. Los filtros de nombre y precio devuelven hasta 100 productos.</p>
      <form onSubmit={ejecutar}>
        <label className="ancho">Consulta
          <select value={tipo} onChange={cambiarTipo}>{consultas.map(([id, titulo]) => <option key={id} value={id}>{titulo}</option>)}</select>
        </label>
        {tipo === 'nombre' && <label className="ancho">El nombre contiene
          <input name="texto" value={campos.texto} onChange={cambiar} required maxLength="100" pattern=".*\S.*" />
        </label>}
        {tipo === 'precios' && <>
          <label>Precio mínimo<input name="minimo" type="number" min="0" step="0.01" value={campos.minimo} onChange={cambiar} required /></label>
          <label>Precio máximo<input name="maximo" type="number" min="0" step="0.01" value={campos.maximo} onChange={cambiar} required /></label>
        </>}
        {tipo === 'costosos' && <label className="ancho">Cantidad de resultados (1–100)
          <input name="limite" type="number" min="1" max="100" step="1" value={campos.limite} onChange={cambiar} required />
        </label>}
        <div className="acciones ancho"><button disabled={cargando}>{cargando ? 'Consultando…' : 'Ejecutar consulta'}</button></div>
      </form>
      {cargando && <p role="status">Ejecutando pipeline…</p>}
      {error && <p role="alert" className="aviso error">{error}</p>}
      {resultado && <div className="resultado">
        <h3>Resultado · {consultas.find(([id]) => id === resultado.filtro)?.[1]}</h3>
        {resultado.filtro === 'estadisticas' ? <dl className="estadisticas">
          {Object.entries(resultado.resultados[0]).map(([key, value]) => <div key={key}>
            <dt>{({ cantidad: 'Productos', precioPromedio: 'Precio promedio', precioMinimo: 'Precio mínimo', precioMaximo: 'Precio máximo' })[key]}</dt>
            <dd>{value ?? 'Sin datos'}</dd>
          </div>)}
        </dl> : <ProductTable productos={resultado.resultados} />}
        <div className="cabecera-lista"><h3>Pipeline ejecutado</h3><button type="button" className="secundario" onClick={copiar}>Copiar pipeline</button></div>
        <pre tabIndex="0" aria-label="Código del pipeline ejecutado"><code>{JSON.stringify(resultado.pipeline, null, 2)}</code></pre>
        {copiado && <p role="status">{copiado}</p>}
      </div>}
    </section>
  )
}
