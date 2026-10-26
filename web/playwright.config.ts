import { defineConfig, devices } from '@playwright/test';

/**
 * Pruebas de punta a punta: el frontend real contra la API real (el jar de api/target).
 * Usan puertos propios (API 8095, web 4300) y H2 en memoria para no tocar lo que esté corriendo.
 * Antes: `cd api && ./mvnw -DskipTests package`. En local usan el Google Chrome instalado; en CI,
 * el Chromium de Playwright.
 */
export default defineConfig({
  testDir: 'e2e',
  timeout: 30_000,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4300',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chrome',
      use: { ...devices['Desktop Chrome'], ...(process.env['CI'] ? {} : { channel: 'chrome' }) },
    },
  ],
  webServer: [
    {
      command:
        'java -jar ../api/target/rips-validator-0.1.0-SNAPSHOT.jar --server.port=8095 ' +
        '--spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1 --rips.limites.validaciones-por-minuto=1000',
      url: 'http://localhost:8095/actuator/health',
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: 'npx ng serve --port 4300 --proxy-config e2e/proxy.e2e.json',
      url: 'http://localhost:4300',
      timeout: 120_000,
      reuseExistingServer: false,
    },
  ],
});
