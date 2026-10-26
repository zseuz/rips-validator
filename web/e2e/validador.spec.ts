import { expect, test } from '@playwright/test';
import path from 'node:path';

const EJEMPLOS = path.resolve(__dirname, '../../ejemplos');

test('un RIPS correcto queda listo para enviar', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'RIPS válido' }).click();

  await expect(page.getByRole('heading', { name: 'Listo para enviar' })).toBeVisible();
  await expect(page.getByText('El archivo pasó todas las reglas.')).toBeVisible();
});

test('al subir un archivo con errores muestra dónde está cada uno', async ({ page }) => {
  await page.goto('/');
  await page.locator('input[type=file][accept^=".json"]').setInputFiles(path.join(EJEMPLOS, 'rips-con-errores.json'));

  await expect(page.getByRole('heading', { name: 'Corrige los errores antes de enviarlo' })).toBeVisible();
  const embarazo = page.locator('.hallazgo', { hasText: 'solo aplica a mujeres' }).first();
  await expect(embarazo).toContainText('Usuario 1');
  await expect(embarazo).toContainText('Diagnóstico principal');

  // Solo advertencias.
  await page.getByRole('group', { name: 'Severidad' }).getByRole('button', { name: 'Advertencias' }).click();
  await expect(page.locator('.hallazgo .insignia', { hasText: 'Error' })).toHaveCount(0);
  await expect(page.locator('.hallazgo').first()).toBeVisible();
});

test('cruza el RIPS con el XML de su factura', async ({ page }) => {
  await page.goto('/');
  await page.locator('input[type=file][accept^=".xml"]').setInputFiles(path.join(EJEMPLOS, 'factura-FEV1001.xml'));
  await expect(page.getByText('factura-FEV1001.xml')).toBeVisible();
  await page.locator('input[type=file][accept^=".json"]').setInputFiles(path.join(EJEMPLOS, 'rips-valido.json'));

  await expect(page.getByRole('heading', { name: 'Listo para enviar' })).toBeVisible();
});

test('el historial lista las validaciones y abre su detalle', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'RIPS con errores' }).click();
  await expect(page.getByText('Rechazado').first()).toBeVisible();

  await page.getByRole('link', { name: 'Historial' }).click();
  await expect(page.getByRole('link', { name: '⬇ Descargar reporte' })).toHaveAttribute('href', '/api/v1/exportaciones/power-bi.zip');
  await page.getByRole('row', { name: /FEV1002/ }).first().click();

  await expect(page).toHaveURL(/\/validaciones\/\d+$/);
  await expect(page.getByRole('heading', { name: 'Corrige los errores antes de enviarlo' })).toBeVisible();
});

test('el tema elegido se conserva al recargar', async ({ page }) => {
  await page.goto('/');
  const html = page.locator('html');
  const inicial = await html.getAttribute('data-tema');
  await page.locator('button.tema').click();
  const cambiado = inicial === 'oscuro' ? 'claro' : 'oscuro';
  await expect(html).toHaveAttribute('data-tema', cambiado);

  await page.reload();
  await expect(html).toHaveAttribute('data-tema', cambiado);
});
