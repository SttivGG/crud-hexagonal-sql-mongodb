export default function ProductTable({ productos, onEdit, onDelete, disabled = false }) {
  return (
    <div className="tabla-contenedor" role="region" aria-label="Tabla de productos" tabIndex="0">
      <table>
        <thead><tr><th scope="col">ID</th><th scope="col">Nombre</th><th scope="col">Precio</th><th scope="col">Descripción</th>
          {onEdit && <th scope="col">Acciones</th>}</tr></thead>
        <tbody>
          {productos.map((producto) => (
            <tr key={producto.id}>
              <td>#{producto.id}</td><td>{producto.name}</td><td className="precio">{producto.price}</td><td>{producto.description}</td>
              {onEdit && <td><div className="acciones-fila">
                <button type="button" className="secundario" disabled={disabled} onClick={() => onEdit(producto)} aria-label={`Editar ${producto.name}`}>Editar</button>
                <button type="button" className="peligro" disabled={disabled} onClick={() => onDelete(producto)} aria-label={`Eliminar ${producto.name}`}>Eliminar</button>
              </div></td>}
            </tr>
          ))}
          {productos.length === 0 && <tr><td className="vacio" colSpan={onEdit ? 5 : 4}>No hay productos para mostrar.</td></tr>}
        </tbody>
      </table>
    </div>
  )
}
