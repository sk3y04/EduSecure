declare module 'qrcode' {
  export type QRCodeErrorCorrectionLevel = 'low' | 'medium' | 'quartile' | 'high' | 'L' | 'M' | 'Q' | 'H'

  export interface QRCodeToDataURLOptions {
    errorCorrectionLevel?: QRCodeErrorCorrectionLevel
    margin?: number
    width?: number
  }

  export function toDataURL(text: string, options?: QRCodeToDataURLOptions): Promise<string>
}

