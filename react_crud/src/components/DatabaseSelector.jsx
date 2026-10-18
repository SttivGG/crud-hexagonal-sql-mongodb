export default function DatabaseSelector({ database, onChange }) {
  return (
    <nav className="selector-db" aria-label="Base de datos">
      {['mysql', 'mongodb'].map((value) => (
        <button key={value} type="button" aria-pressed={database === value}
          className={database === value ? 'seleccionado' : 'secundario'} onClick={() => onChange(value)}>
          {value === 'mysql' ? 'MySQL' : 'MongoDB'}
          <small>{value === 'mysql' ? 'SQL · Tablas' : 'NoSQL · Documentos'}</small>
        </button>
      ))}
    </nav>
  )
}
