import { act, fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import MongoFilters from '../components/MongoFilters.jsx'

it('muestra y copia el pipeline exacto devuelto por el backend', async () => {
  const pipeline = [{ $match: { price: { $gte: { $numberDecimal: '0.10' }, $lte: { $numberDecimal: '10.20' } } } }]
  const fetchMock = vi.fn(async () => ({ ok: true, status: 200, json: async () => ({ filtro: 'precios', resultados: [], pipeline }) }))
  vi.stubGlobal('fetch', fetchMock)
  const writeText = vi.fn().mockResolvedValue()
  Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
  render(<MongoFilters />)
  fireEvent.change(screen.getByLabelText('Consulta'), { target: { value: 'precios' } })
  fireEvent.change(screen.getByLabelText('Precio mínimo'), { target: { value: '0.10' } })
  fireEvent.change(screen.getByLabelText('Precio máximo'), { target: { value: '10.20' } })
  fireEvent.click(screen.getByRole('button', { name: 'Ejecutar consulta' }))
  const code = await screen.findByLabelText('Código del pipeline ejecutado')
  expect(code.textContent).toBe(JSON.stringify(pipeline, null, 2))
  expect(fetchMock.mock.calls[0][0]).toContain('/filtros/precios?minimo=0.10&maximo=10.20')
  fireEvent.click(screen.getByRole('button', { name: 'Copiar pipeline' }))
  await screen.findByText('Pipeline copiado.')
  expect(writeText).toHaveBeenCalledWith(JSON.stringify(pipeline, null, 2))
})

it('valida rangos incorrectos antes de consultar', async () => {
  const fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  render(<MongoFilters />)
  fireEvent.change(screen.getByLabelText('Consulta'), { target: { value: 'precios' } })
  fireEvent.change(screen.getByLabelText('Precio mínimo'), { target: { value: '20' } })
  fireEvent.change(screen.getByLabelText('Precio máximo'), { target: { value: '10' } })
  fireEvent.click(screen.getByRole('button', { name: 'Ejecutar consulta' }))
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(fetchMock).not.toHaveBeenCalled()
})

it('descarta consultas anteriores al elegir otro filtro', async () => {
  let resolver
  let signal
  vi.stubGlobal('fetch', vi.fn((url, options) => { signal = options.signal; return new Promise((resolve) => { resolver = resolve }) }))
  render(<MongoFilters />)
  fireEvent.change(screen.getByLabelText('El nombre contiene'), { target: { value: 'Mouse' } })
  fireEvent.click(screen.getByRole('button', { name: 'Ejecutar consulta' }))
  fireEvent.change(screen.getByLabelText('Consulta'), { target: { value: 'estadisticas' } })
  expect(signal.aborted).toBe(true)
  await act(async () => { resolver({ ok: true, status: 200, json: async () => ({ filtro: 'nombre', resultados: [], pipeline: [] }) }) })
  expect(screen.queryByText('Pipeline ejecutado')).toBeNull()
})
