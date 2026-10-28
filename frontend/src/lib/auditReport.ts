import { AuditResponse } from '../types';

const SENSITIVE_KEY = /^(authorization|cookie|password|secret|token|set-cookie|api[-_]?key|credentials)$/i;

type ExportValue = null | boolean | number | string | ExportValue[] | { [key: string]: ExportValue };

export function sanitizeAuditForExport(value: unknown): ExportValue {
  if (Array.isArray(value)) return value.map(sanitizeAuditForExport);
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([key]) => !SENSITIVE_KEY.test(key))
        .map(([key, entry]) => [key, sanitizeAuditForExport(entry)])
    );
  }
  if (value === undefined) return null;
  return value as ExportValue;
}

export function createAuditJsonBlob(audit: AuditResponse): Blob {
  return new Blob([JSON.stringify(sanitizeAuditForExport(audit), null, 2)], {
    type: 'application/json;charset=utf-8'
  });
}

function display(value: unknown): string {
  return value === undefined || value === null || value === '' ? 'Not collected' : String(value);
}

function reportLines(audit: AuditResponse): string[] {
  const lines = [
    'LaunchCheck Full Audit Report',
    `Audit ID: ${display(audit.auditId)}`,
    `Audit date: ${display(audit.startedAt)}`,
    `Completed: ${display(audit.completedAt)}`,
    `Overall status: ${display(audit.overallStatus)}`,
    `Duration: ${display(audit.durationMs)} ms`,
    ''
  ];

  audit.targets?.forEach((target, index) => {
    lines.push(`Target ${index + 1}: ${display(target.role)}`, `URL/host: ${display(target.host)}`,
      `Status: ${display(target.status)}`, `HTTP status: ${display(target.httpStatus)}`,
      `Response time: ${display(target.responseTimeMs)} ms`, `Diagnostic: ${display(target.diagnostic)}`,
      `Next step: ${display(target.nextStep)}`,
      `TLS status: ${display(target.tls?.status)}`,
      `Certificate expires: ${display(target.tls?.certificateExpiresAt)}`,
      `TLS days remaining: ${display(target.tls?.daysRemaining)}`,
      `TLS diagnostic: ${display(target.tls?.diagnostic)}`);
    if (target.securityHeaders?.length) {
      target.securityHeaders.forEach((header) => lines.push(
        `Security header ${header.headerName}: ${display(header.status)}`,
        `  Value: ${display(header.value)}`,
        `  Diagnostic: ${display(header.diagnostic)}`
      ));
    } else lines.push('Security headers: Not collected');
    lines.push('');
  });

  if (audit.cors) {
    lines.push('CORS diagnostics', `Endpoint: ${display(audit.cors.endpointPath)}`,
      `Origin: ${display(audit.cors.origin)}`, `Method: ${display(audit.cors.method)}`,
      `Preflight attempted: ${display(audit.cors.preflightAttempted)}`,
      `Status: ${display(audit.cors.status)}`, `Diagnostic: ${display(audit.cors.diagnostic)}`,
      `Allow-Origin: ${display(audit.cors.responseHeaders?.allowOrigin)}`,
      `Allow-Methods: ${display(audit.cors.responseHeaders?.allowMethods)}`,
      `Allow-Headers: ${display(audit.cors.responseHeaders?.allowHeaders)}`,
      `Allow-Credentials: ${display(audit.cors.responseHeaders?.allowCredentials)}`,
      `Vary: ${display(audit.cors.responseHeaders?.vary)}`, '');
  } else lines.push('CORS: Not collected', '');

  if (audit.apiTests?.length) {
    lines.push('API smoke tests');
    audit.apiTests.forEach((test, index) => lines.push(
      `Test ${index + 1}: ${display(test.method)} ${display(test.path)}`,
      `Expected status: ${display(test.expectedStatus)}`,
      `Actual status: ${display(test.actualStatus)}`,
      `Status: ${display(test.status)}`,
      `Diagnostic: ${display(test.diagnostic)}`,
      `JSON assertion passed: ${display(test.jsonAssertionPassed)}`,
      `JSON assertion diagnostic: ${display(test.jsonAssertionDiagnostic)}`
    ));
  } else lines.push('API smoke tests: Not collected');

  return lines;
}

function pdfText(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)')
    .replace(/[^\x20-\x7E]/g, '?');
}

export function createAuditPdfBlob(audit: AuditResponse): Blob {
  const wrappedLines = reportLines(audit).flatMap((line) => {
    if (!line) return [''];
    const chunks: string[] = [];
    for (let index = 0; index < line.length; index += 96) chunks.push(line.slice(index, index + 96));
    return chunks;
  });
  const pageLines = 42;
  const pages = Array.from({ length: Math.max(1, Math.ceil(wrappedLines.length / pageLines)) }, (_, index) =>
    wrappedLines.slice(index * pageLines, (index + 1) * pageLines)
  );
  const fontObject = 3;
  const pageObjectStart = 4;
  const contentObjectStart = pageObjectStart + pages.length;
  const pageObjects = pages.map((page, pageIndex) => {
    const contentObject = contentObjectStart + pageIndex;
    const content = [
      'BT', pageIndex === 0 ? '/F1 16 Tf' : '/F1 9 Tf', '50 760 Td',
      ...page.map((line, lineIndex) => `${lineIndex === 0 ? '' : '0 -16 Td '}(${pdfText(line)}) Tj`),
      'ET'
    ].join('\n');
    return {
      page: `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 ${fontObject} 0 R >> >> /Contents ${contentObject} 0 R >>`,
      content: `<< /Length ${content.length} >>\nstream\n${content}\nendstream`
    };
  });
  const objects = [
    `<< /Type /Catalog /Pages 2 0 R >>`,
    `<< /Type /Pages /Kids [${pageObjects.map((_, index) => `${pageObjectStart + index} 0 R`).join(' ')}] /Count ${pages.length} >>`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    ...pageObjects.map((entry) => entry.page),
    ...pageObjects.map((entry) => entry.content)
  ];
  let pdf = '%PDF-1.4\n';
  const offsets = [0];
  objects.forEach((object, index) => {
    offsets.push(pdf.length);
    pdf += `${index + 1} 0 obj\n${object}\nendobj\n`;
  });
  const xrefOffset = pdf.length;
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  offsets.slice(1).forEach((offset) => { pdf += `${String(offset).padStart(10, '0')} 00000 n \n`; });
  pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`;
  return new Blob([pdf], { type: 'application/pdf' });
}

export function downloadAuditBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function auditFilename(audit: AuditResponse, extension: 'pdf' | 'json'): string {
  const date = new Date(audit.startedAt);
  const datePart = Number.isNaN(date.getTime()) ? 'undated' : date.toISOString().slice(0, 10);
  return `launchcheck-audit-${datePart}.${extension}`;
}
