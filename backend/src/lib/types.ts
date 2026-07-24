/** Makes all properties of T nullable */
export type Nullable<T> = { [K in keyof T]: T[K] | null };

/** Makes all properties of T optional recursively */
export type DeepPartial<T> = T extends object
  ? { [P in keyof T]?: DeepPartial<T[P]> }
  : T;

/** Removes null and undefined from union types */
export type NonNullable<T> = T extends null | undefined ? never : T;

/** Extracts the type of a value in a Map-like structure */
export type MapValue<T> = T extends Map<unknown, infer V> ? V : never;
