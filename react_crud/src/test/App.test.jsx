import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from '../App.jsx'

const sql = { id: 1, name: 'Producto SQL', price: 10.25, description: 'Tabla' }
const nosql = { id: 1, name: 'Producto Mongo', price: 20.50, description: 'Documento' }
const response = (data, status = 200) => ({ ok: status < 400, status, json: async () => data, text: async () => JSON.stringify(data) })

describe('Selección de persistencia y CRUD', () => {
  it('ignora una respuesta SQL tardía al cambiar a MongoDB y limpia el formulario', async () => {
    let resolverSql
    let sqlSignal
    vi.stubGlobal('fetch', vi.fn((url, options) => {
      if (url.includes('/api/mysql/')) {
        sqlSignal = options.signal
        return new Promise((resolve) => { resolverSql = resolve })
      }
      return Promise.resolve(response([nosql]))
    }))
    render(<App />)
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Borrador' } })
    fireEvent.click(screen.getByRole('button', { name: /MongoDB/ }))
    await screen.findByText('Producto Mongo')
    expect(screen.getByLabelText('Nombre').value).toBe('')
    expect(sqlSignal.aborted).toBe(true)
    await act(async () => { resolverSql(response([sql])) })
    expect(screen.queryByText('Producto SQL')).toBeNull()
    expect(screen.getByText('Explorar productos')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /MySQL/ }))
    expect(screen.queryByText('Explorar productos')).toBeNull()
    expect(screen.queryByText('Producto Mongo')).toBeNull()
  })

  it.each(['mysql', 'mongodb'])('crea, edita y elimina usando la API de %s', async (database) => {
    let productos = []
    const fetchMock = vi.fn(async (url, options = {}) => {
      if (options.method === 'POST') {
        const datos = JSON.parse(options.body)
        expect(datos.price).toBe('0.10')
        productos = [{ ...datos, id: 7 }]
        return response(productos[0], 201)
      }
      if (options.method === 'PUT') {
        productos = [{ ...JSON.parse(options.body), id: 7 }]
        return response(productos[0])
      }
      if (options.method === 'DELETE') { productos = []; return response(null, 204) }
      return response(productos)
    })
    vi.stubGlobal('fetch', fetchMock)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<App />)
    if (database === 'mongodb') fireEvent.click(screen.getByRole('button', { name: /MongoDB/ }))
    await screen.findByText('No hay productos para mostrar.')
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Mouse' } })
    fireEvent.change(screen.getByLabelText('Precio'), { target: { value: '0.10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear producto' }))
    await screen.findByText('Mouse')
    fireEvent.click(screen.getByRole('button', { name: 'Editar Mouse' }))
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Mouse USB' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await screen.findByText('Mouse USB')
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Mouse USB' }))
    await screen.findByText('Producto eliminado.')
    await waitFor(() => expect(screen.queryByText('Mouse USB')).toBeNull())
    for (const [url, options] of fetchMock.mock.calls.filter(([, options]) => options.method)) {
      expect(url).toContain(`/api/${database}/productos`)
    }
  })

  it('muestra errores y permite reintentar la carga', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValue(response([sql])))
    render(<App />)
    await screen.findByRole('alert')
    fireEvent.click(screen.getByRole('button', { name: 'Actualizar lista' }))
    await screen.findByText('Producto SQL')
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
