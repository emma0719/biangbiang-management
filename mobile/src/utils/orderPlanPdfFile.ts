import { Linking, Platform, Share } from 'react-native';
import { ApiBinaryResponse } from '../api/client';

export type PdfViewTarget = Window | null | undefined;

export function preparePdfViewTarget(): PdfViewTarget {
  if (Platform.OS !== 'web') return undefined;
  return window.open('', '_blank');
}

export async function viewPdfFile(file: ApiBinaryResponse, filename: string, target?: PdfViewTarget) {
  ensurePdf(file);
  if (Platform.OS === 'web') {
    const url = createObjectUrl(file);
    if (target) {
      target.location.replace(url);
      setTimeout(() => URL.revokeObjectURL(url), 60000);
      return;
    }
    throw new Error('PDF_POPUP_BLOCKED');
  }
  await Linking.openURL(toDataUri(file));
}

export async function downloadPdfFile(file: ApiBinaryResponse, filename: string) {
  ensurePdf(file);
  const safeFilename = toSafeFilename(filename);
  if (Platform.OS === 'web') {
    const url = createObjectUrl(file);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = safeFilename;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
    return;
  }
  await Share.share({ title: safeFilename, url: toDataUri(file) });
}

function ensurePdf(file: ApiBinaryResponse) {
  if (!file.mimeType.toLowerCase().includes('pdf')) throw new Error('PDF_INVALID_MIME');
}

function createObjectUrl(file: ApiBinaryResponse) {
  const blob = file.blob ?? new Blob([file.arrayBuffer], { type: file.mimeType });
  return URL.createObjectURL(blob);
}

function toDataUri(file: ApiBinaryResponse) {
  return `data:${file.mimeType};base64,${arrayBufferToBase64(file.arrayBuffer)}`;
}

function arrayBufferToBase64(buffer: ArrayBuffer) {
  const bytes = new Uint8Array(buffer);
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  let output = '';
  for (let index = 0; index < bytes.length; index += 3) {
    const first = bytes[index];
    const second = bytes[index + 1];
    const third = bytes[index + 2];
    output += alphabet[first >> 2];
    output += alphabet[((first & 3) << 4) | ((second ?? 0) >> 4)];
    output += second === undefined ? '=' : alphabet[((second & 15) << 2) | ((third ?? 0) >> 6)];
    output += third === undefined ? '=' : alphabet[third & 63];
  }
  return output;
}

function toSafeFilename(filename: string) {
  return filename.replace(/[\\/:*?"<>|]+/g, '_') || 'order-plan.pdf';
}
