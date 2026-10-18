import { useEffect, useRef, useState } from 'react'
import DatabaseSelector from './components/DatabaseSelector.jsx'
import ProductForm from './components/ProductForm.jsx'
import ProductTable from './components/ProductTable.jsx'
import MongoFilters from './components/MongoFilters.jsx'
import { rutaProductos, solicitar } from './services/api.js'

const formularioVacio = { name: '', price: '', description: '' }

// Cada cambio de tecnología desmonta la sesión y cancela sus peticiones.
function ProductWorkspace({ database }) {
  const [productos, setProductos] = useState([])
  const [formulario, setFormulario] = useState(formularioVacio)
  const [editandoId, setEditandoId] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [revision, setRevision] = useState(0)
  const vida = useRef(null)
  const lectura = useRef(null)
  const base = rutaProductos(database)

  useEffect(() => {
    const controller = new AbortController()
    vida.current = controller
    cargarProductos(controller.signal)
    return () => { controller.abort(); lectura.current?.abort() }
  }, [database])

  async function cargarProductos(signal = vida.current?.signal) {
    lectura.current?.abort()
    const controller = new AbortController()
    lectura.current = controller
    setCargando(true)
    try {
      const datos = await solicitar(base, { signal: controller.signal })
      if (!signal?.aborted && !controller.signal.aborted) { setProductos(datos); setError('') }
    } catch (fallo) {
      if (!signal?.aborted && !controller.signal.aborted) setError(fallo.message)
    } finally {
      if (!signal?.aborted && !controller.signal.aborted) setCargando(false)
    }
  }

  function cancelar() { setEditandoId(null); setFormulario(formularioVacio) }

  function editar(producto) {
    setEditandoId(producto.id)
    setFormulario({ name: producto.name, price: String(producto.price), description: producto.description })
    setMensaje('')
    setError('')
    document.querySelector('input[name="name"]')?.focus()
  }

  async function mutar(operacion, exito) {
    const signal = vida.current.signal
    setGuardando(true)
    setError('')
    setMensaje('')
    try {
      await operacion(signal)
      if (signal.aborted) return
      cancelar()
      setMensaje(exito)
      setRevision((anterior) => anterior + 1)
      await cargarProductos(signal)
    } catch (fallo) {
      if (!signal.aborted) setError(fallo.message)
    } finally {
      if (!signal.aborted) setGuardando(false)
    }
  }

  async function guardar(evento) {
    evento.preventDefault()
    if (!formulario.name.trim()) { setError('El nombre es obligatorio.'); return }
    // Enviar una cadena decimal evita redondear el importe con Number antes de llegar a Java.
    const producto = { name: formulario.name.trim(), price: formulario.price, description: formulario.description.trim() }
    await mutar((signal) => solicitar(editandoId === null ? base : `${base}/${editandoId}`, {
      method: editandoId === null ? 'POST' : 'PUT', signal,
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(producto)
    }), editandoId === null ? 'Producto creado.' : 'Producto actualizado.')
  }

  async function eliminar(producto) {
    if (!window.confirm(`¿Eliminar ${producto.name} de ${database === 'mysql' ? 'MySQL' : 'MongoDB'}?`)) return
    await mutar((signal) => solicitar(`${base}/${producto.id}`, { method: 'DELETE', signal }), 'Producto eliminado.')
  }

  return <>
    <ProductForm formulario={formulario} editandoId={editandoId} guardando={guardando}
      onChange={(evento) => setFormulario({ ...formulario, [evento.target.name]: evento.target.value })}
      onSubmit={guardar} onCancel={cancelar} />
    {error && <p role="alert" className="aviso error">{error}</p>}
    {mensaje && <p role="status" className="aviso exito">{mensaje}</p>}
    <section className="tarjeta" aria-labelledby="titulo-lista" aria-busy={cargando}>
      <div className="cabecera-lista">
        <h2 id="titulo-lista">Productos <span className="contador">{productos.length}</span></h2>
        <button type="button" className="secundario" disabled={cargando || guardando} onClick={() => cargarProductos()}>Actualizar lista</button>
      </div>
      {cargando ? <p role="status">Cargando productos…</p> : <ProductTable productos={productos} onEdit={editar} onDelete={eliminar} disabled={guardando} />}
    </section>
    {database === 'mongodb' && <MongoFilters key={revision} />}
  </>
}

export default function App() {
  const [database, setDatabase] = useState('mysql')
  return <main className="contenedor" data-database={database}>
    <header>
      <p className="etiqueta">Taller · Arquitectura hexagonal</p>
      <h1>CRUD de productos</h1>
      <p>Administra tu catálogo y explora sus datos.</p>
      <DatabaseSelector database={database} onChange={setDatabase} />
      <p className="tecnologia" role="status">Persistencia activa: <strong>{database === 'mysql' ? 'MySQL · SQL' : 'MongoDB · NoSQL'}</strong></p>
    </header>
    <ProductWorkspace key={database} database={database} />
  </main>
}
