export default function ProductForm({ formulario, editandoId, guardando, onChange, onSubmit, onCancel }) {
  return (
    <section className="tarjeta" aria-labelledby="titulo-formulario">
      <h2 id="titulo-formulario">{editandoId === null ? 'Nuevo producto' : `Editar producto #${editandoId}`}</h2>
      <form onSubmit={onSubmit}>
        <fieldset disabled={guardando} className="campos-producto">
          <label>Nombre
            <input name="name" value={formulario.name} onChange={onChange} required maxLength="100" pattern=".*\S.*" />
          </label>
          <label>Precio
            <input name="price" type="number" value={formulario.price} onChange={onChange} required min="0" step="0.01" />
          </label>
          <label className="ancho">Descripción
            <input name="description" value={formulario.description} onChange={onChange} maxLength="255" />
          </label>
          <div className="acciones ancho">
            <button type="submit">{guardando ? 'Guardando…' : editandoId === null ? 'Crear producto' : 'Guardar cambios'}</button>
            {editandoId !== null && <button type="button" className="secundario" onClick={onCancel}>Cancelar</button>}
          </div>
        </fieldset>
      </form>
    </section>
  )
}
