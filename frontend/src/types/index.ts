export interface UserSummary {
  id: string;
  email: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserSummary;
}

export interface Project {
  id: string;
  name: string;
  frontendUrl: string;
  backendUrl: string;
  createdAt: string;
}

export interface HeaderAssertion {
  name: string;
  value: string;
}

export interface JsonAssertion {
  jsonPointer: string;
  expectedValue: string;
}

export interface ApiCheck {
  path: string;
  method: string;
  expectedStatus: number;
  headerAssertions: HeaderAssertion[];
  jsonAssertions: JsonAssertion[];
}

export interface TlsResult {
  status: 'PASS' | 'WARN' | 'FAIL' | 'SKIPPED';
  certificateExpiresAt?: string;
  daysRemaining?: number;
  diagnostic: string;
}

export interface RedirectInfo {
  statusCode: number;
  host: string;
  scheme: string;
}

export interface SecurityHeaderAssessment {
  headerName: string;
  status: 'PASS' | 'WARN' | 'FAIL' | 'SKIPPED';
  value?: string;
  diagnostic: string;
}

export interface TargetResult {
  role: 'FRONTEND' | 'BACKEND';
  host: string;
  status: 'PASS' | 'WARN' | 'FAIL' | 'SKIPPED';
  httpStatus?: number;
  responseTimeMs?: number;
  tls: TlsResult;
  redirects: RedirectInfo[];
  diagnostic: string;
  nextStep: string;
  securityHeaders?: SecurityHeaderAssessment[];
}

export interface ApiSmokeTestResult {
  path: string;
  method: string;
  expectedStatus: number;
  actualStatus?: number;
  status: 'PASS' | 'WARN' | 'FAIL';
  diagnostic: string;
  nextStep: string;
  jsonAssertionPassed?: boolean;
  jsonAssertionDiagnostic?: string;
}

export interface CorsResponseHeaders {
  allowOrigin: string;
  allowMethods: string;
  allowHeaders: string;
  allowCredentials: string;
  vary: string;
}

export interface CorsDiagnosticResult {
  endpointPath: string;
  method: string;
  origin: string;
  status: 'PASS' | 'WARN' | 'FAIL';
  diagnostic: string;
  preflightAttempted: boolean;
  responseHeaders?: CorsResponseHeaders;
}

export interface AuditResponse {
  auditId: string;
  startedAt: string;
  completedAt: string;
  durationMs: number;
  overallStatus: 'PASS' | 'WARN' | 'FAIL';
  targets: TargetResult[];
  apiTests?: ApiSmokeTestResult[];
  cors?: CorsDiagnosticResult;
}

export interface AuditSummary {
  id: string;
  projectId?: string;
  startedAt: string;
  completedAt: string;
  durationMs: number;
  overallStatus: 'PASS' | 'WARN' | 'FAIL';
  frontendStatus?: string;
  frontendHttpStatus?: number;
  backendStatus?: string;
  backendHttpStatus?: number;
  corsStatus?: string;
  apiSmokeTestCount: number;
  apiSmokeTestPassedCount: number;
}

export interface SingleSmokeTestResponse {
  success: boolean;
  status: 'PASS' | 'WARN' | 'FAIL';
  expectedStatus: number;
  actualStatus?: number;
  latencyMs: number;
  diagnostic?: string;
  errorMessage?: string;
  jsonAssertionPassed?: boolean;
  jsonAssertionDiagnostic?: string;
}

export interface PaginatedAudits {
  content: AuditSummary[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
