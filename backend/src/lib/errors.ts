export enum ErrorCode {
  NOT_FOUND = 'NOT_FOUND',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  UNAUTHORIZED = 'UNAUTHORIZED',
  FORBIDDEN = 'FORBIDDEN',
  CONFLICT = 'CONFLICT',
  TOO_MANY_REQUESTS = 'TOO_MANY_REQUESTS',
  INTERNAL_ERROR = 'INTERNAL_ERROR',
  NOT_IMPLEMENTED = 'NOT_IMPLEMENTED',
  SERVICE_UNAVAILABLE = 'SERVICE_UNAVAILABLE',
INVALID_PARAMS = 'INVALID_PARAMS',
  INVALID_PLAYBACK_SESSION = 'INVALID_PLAYBACK_SESSION',
  DRAMA_NOT_FOUND = 'DRAMA_NOT_FOUND',
  EPISODE_NOT_FOUND = 'EPISODE_NOT_FOUND',
  EPISODE_NOT_PLAYABLE = 'EPISODE_NOT_PLAYABLE',
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  CANNOT_MODIFY_SELF = 'CANNOT_MODIFY_SELF',
}

const ErrorStatusCode: Record<ErrorCode, number> = {
  [ErrorCode.NOT_FOUND]: 404,
  [ErrorCode.VALIDATION_ERROR]: 400,
  [ErrorCode.UNAUTHORIZED]: 401,
  [ErrorCode.FORBIDDEN]: 403,
  [ErrorCode.CONFLICT]: 409,
  [ErrorCode.TOO_MANY_REQUESTS]: 429,
  [ErrorCode.INTERNAL_ERROR]: 500,
  [ErrorCode.NOT_IMPLEMENTED]: 501,
  [ErrorCode.SERVICE_UNAVAILABLE]: 503,
[ErrorCode.INVALID_PARAMS]: 400,
  [ErrorCode.INVALID_PLAYBACK_SESSION]: 400,
  [ErrorCode.DRAMA_NOT_FOUND]: 404,
  [ErrorCode.EPISODE_NOT_FOUND]: 404,
  [ErrorCode.EPISODE_NOT_PLAYABLE]: 409,
  [ErrorCode.INVALID_CREDENTIALS]: 401,
  [ErrorCode.CANNOT_MODIFY_SELF]: 400,
};

export class AppError extends Error {
  public readonly code: ErrorCode;
  public readonly statusCode: number;
  public readonly details?: unknown;

  constructor(code: ErrorCode, message: string, details?: unknown) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.statusCode = ErrorStatusCode[code];
    this.details = details;
  }
}

export const Errors = {
  notFound: (resource: string, id: string) =>
    new AppError(ErrorCode.NOT_FOUND, `${resource} (${id}) not found`),

  validationError: (message: string, details?: unknown) =>
    new AppError(ErrorCode.VALIDATION_ERROR, message, details),

  invalidParams: (message: string, details?: unknown) =>
    new AppError(ErrorCode.INVALID_PARAMS, message, details),

  invalidPlaybackSession: (message = 'Playback session is invalid') =>
    new AppError(ErrorCode.INVALID_PLAYBACK_SESSION, message),

  dramaNotFound: (dramaId: string) =>
    new AppError(ErrorCode.DRAMA_NOT_FOUND, `Drama (${dramaId}) not found`),

  episodeNotFound: (episodeId: string) =>
    new AppError(ErrorCode.EPISODE_NOT_FOUND, `Episode (${episodeId}) not found`),

  episodeNotPlayable: (episodeId: string) =>
    new AppError(ErrorCode.EPISODE_NOT_PLAYABLE, `Episode (${episodeId}) has no playable resource`),

  unauthorized: (message = 'Authentication required') =>
    new AppError(ErrorCode.UNAUTHORIZED, message),

  forbidden: (message = 'Access denied') =>
    new AppError(ErrorCode.FORBIDDEN, message),

  conflict: (message: string) =>
    new AppError(ErrorCode.CONFLICT, message),

  tooManyRequests: (message = 'Too many requests') =>
    new AppError(ErrorCode.TOO_MANY_REQUESTS, message),

  internal: (message = 'Internal server error') =>
    new AppError(ErrorCode.INTERNAL_ERROR, message),

  notImplemented: (message = 'Not implemented') =>
    new AppError(ErrorCode.NOT_IMPLEMENTED, message),

  serviceUnavailable: (service: string) =>
    new AppError(ErrorCode.SERVICE_UNAVAILABLE, `Service unavailable: ${service}`),

  invalidCredentials: (message = 'Invalid email or password') =>
    new AppError(ErrorCode.INVALID_CREDENTIALS, message),

  cannotModifySelf: (message = 'Cannot modify your own role') =>
    new AppError(ErrorCode.CANNOT_MODIFY_SELF, message),
};

export interface ErrorResponseBody {
  error: {
    code: string;
    message: string;
  };
}

export function formatErrorResponse(err: AppError): ErrorResponseBody {
  return {
    error: {
      code: err.code,
      message: err.message,
    },
  };
}
