import { expect, test } from '@playwright/test';
import path from 'node:path';

const EJEMPLOS = path.resolve(__dirname, '../../ejemplos');
const ejemplo = (nombre: string) => path.join(EJEMPLOS, nombre);

test('una cuenta correcta queda lista para radicar', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'cuenta correcta' }).click();

  await expect(page.getByRole('heading', { name: 'Validación aprobada' })).toBeVisible();
  await expect(page.getByText('El archivo pasó todas las reglas y cuadra con su factura.')).toBeVisible();
});

test('exige el RIPS y la factura antes de validar', async ({ page }) => {
  await page.goto('/');
  const validar = page.getByRole('button', { name: 'Validar', exact: true });
  await expect(validar).toBeDisabled();
  await expect(page.getByText('Falta el RIPS y la factura.')).toBeVisible();

  await page.getByLabel('RIPS de la factura').setInputFiles(ejemplo('rips-con-errores.json'));
  await expect(validar).toBeDisabled();
  await expect(page.getByText('Falta la factura.')).toBeVisible();

  await page.getByLabel('Factura electrónica').setInputFiles(ejemplo('factura-FEV1002.xml'));
  await expect(validar).toBeEnabled();
  await validar.click();

  await expect(page.getByRole('heading', { name: 'Validación rechazada' })).toBeVisible();
  const embarazo = page.locator('.hallazgo', { hasText: 'solo aplica a mujeres' }).first();
  await expect(embarazo).toContainText('Usuario 1');
  await expect(embarazo).toContainText('Diagnóstico principal');

  // El hallazgo se despliega para ver el registro afectado.
  const desplegar = embarazo.locator('button[aria-expanded]');
  await desplegar.click();
  await expect(desplegar).toHaveAttribute('aria-expanded', 'true');
  await expect(embarazo.locator('.ruta')).toContainText('usuarios[0]');

  // Solo advertencias.
  await page.getByRole('group', { name: 'Severidad' }).getByRole('button', { name: 'Advertencias' }).click();
  await expect(page.locator('.hallazgo .estado', { hasText: /^Error$/ })).toHaveCount(0);
  await expect(page.locator('.hallazgo').first()).toBeVisible();
});

test('rechaza archivos con la extensión equivocada', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('RIPS de la factura').setInputFiles(ejemplo('factura-FEV1001.xml'));
  await expect(page.getByRole('alert')).toContainText('no es un .json');
});

test('detecta un RIPS que no corresponde a su factura', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('RIPS de la factura').setInputFiles(ejemplo('rips-valido.json'));
  await page.getByLabel('Factura electrónica').setInputFiles(ejemplo('factura-FEV1002.xml'));
  await page.getByRole('button', { name: 'Validar', exact: true }).click();

  await expect(page.getByRole('heading', { name: 'Validación rechazada' })).toBeVisible();
  await expect(page.locator('.hallazgo', { hasText: 'La factura electrónica adjunta es la FEV1002' })).toBeVisible();
});

test('el historial lista las validaciones y abre su detalle', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'cuenta con errores' }).click();
  await expect(page.getByRole('heading', { name: 'Validación rechazada' })).toBeVisible();

  await page.getByRole('link', { name: 'Historial' }).click();
  await expect(page.getByRole('link', { name: 'Descargar reporte' })).toHaveAttribute('href', '/api/v1/exportaciones/power-bi.zip');
  await page.getByRole('row', { name: /FEV1002/ }).first().click();

  await expect(page).toHaveURL(/\/validaciones\/\d+$/);
  await expect(page.getByRole('heading', { name: 'Validación rechazada' })).toBeVisible();
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
