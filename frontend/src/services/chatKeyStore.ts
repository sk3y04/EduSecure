import type { ChatKeyAlgorithm, ChatKeyPairMaterial } from '@/services/chatCrypto'

const DB_NAME = 'edusecure-chat-crypto'
const DB_VERSION = 1
const CHAT_KEY_STORE = 'chatKeys'
const ROOM_KEY_STORE = 'roomKeys'

interface StoredChatKeyRecord {
  userId: string
  algorithm: ChatKeyAlgorithm
  fingerprint: string
  publicKeyJwkJson: string
  privateKey: CryptoKey
  createdAt: string
}

interface StoredRoomKeyRecord {
  id: string
  userId: string
  spaceId: string
  keyVersion: number
  roomKey: CryptoKey
  createdAt: string
}

export interface StoredChatKeyMetadata {
  userId: string
  algorithm: ChatKeyAlgorithm
  fingerprint: string
  publicKeyJwkJson: string
  createdAt: string
}

export interface StoredChatKeyMaterial extends StoredChatKeyMetadata {
  privateKey: CryptoKey
}

function ensureIndexedDb(): IDBFactory {
  if (!globalThis.indexedDB) {
    throw new Error('This browser does not support IndexedDB required for encrypted chat key storage.')
  }
  return globalThis.indexedDB
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = ensureIndexedDb().open(DB_NAME, DB_VERSION)

    request.onerror = () => {
      reject(request.error ?? new Error('Unable to open encrypted chat key storage.'))
    }

    request.onupgradeneeded = () => {
      const database = request.result

      if (!database.objectStoreNames.contains(CHAT_KEY_STORE)) {
        database.createObjectStore(CHAT_KEY_STORE, { keyPath: 'userId' })
      }

      if (!database.objectStoreNames.contains(ROOM_KEY_STORE)) {
        const roomKeyStore = database.createObjectStore(ROOM_KEY_STORE, { keyPath: 'id' })
        roomKeyStore.createIndex('byUserSpaceVersion', ['userId', 'spaceId', 'keyVersion'], { unique: true })
      }
    }

    request.onsuccess = () => {
      resolve(request.result)
    }
  })
}

async function withStore<T>(
  storeName: string,
  mode: IDBTransactionMode,
  action: (store: IDBObjectStore) => Promise<T> | T,
): Promise<T> {
  const database = await openDatabase()

  try {
    const transaction = database.transaction(storeName, mode)
    const store = transaction.objectStore(storeName)
    const result = await action(store)

    await new Promise<void>((resolve, reject) => {
      transaction.oncomplete = () => resolve()
      transaction.onerror = () => reject(transaction.error ?? new Error('Encrypted chat storage transaction failed.'))
      transaction.onabort = () => reject(transaction.error ?? new Error('Encrypted chat storage transaction was aborted.'))
    })

    return result
  } finally {
    database.close()
  }
}

function requestToPromise<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('Encrypted chat storage request failed.'))
  })
}

export const chatKeyStore = {
  async saveCurrentUserKey(userId: string, material: ChatKeyPairMaterial): Promise<void> {
    const record: StoredChatKeyRecord = {
      userId,
      algorithm: material.algorithm,
      fingerprint: material.fingerprint,
      publicKeyJwkJson: material.publicKeyJwkJson,
      privateKey: material.privateKey,
      createdAt: material.createdAt,
    }

    await withStore(CHAT_KEY_STORE, 'readwrite', async (store) => {
      await requestToPromise(store.put(record))
    })
  },

  async loadCurrentUserKey(userId: string): Promise<StoredChatKeyMaterial | null> {
    return withStore(CHAT_KEY_STORE, 'readonly', async (store) => {
      const record = await requestToPromise(store.get(userId) as IDBRequest<StoredChatKeyRecord | undefined>)
      if (!record) {
        return null
      }

      return {
        userId: record.userId,
        algorithm: record.algorithm,
        fingerprint: record.fingerprint,
        publicKeyJwkJson: record.publicKeyJwkJson,
        privateKey: record.privateKey,
        createdAt: record.createdAt,
      }
    })
  },

  async clearCurrentUserKey(userId: string): Promise<void> {
    await withStore(CHAT_KEY_STORE, 'readwrite', async (store) => {
      await requestToPromise(store.delete(userId))
    })
  },

  async saveRoomKey(userId: string, spaceId: string, keyVersion: number, roomKey: CryptoKey): Promise<void> {
    const record: StoredRoomKeyRecord = {
      id: `${userId}:${spaceId}:${keyVersion}`,
      userId,
      spaceId,
      keyVersion,
      roomKey,
      createdAt: new Date().toISOString(),
    }

    await withStore(ROOM_KEY_STORE, 'readwrite', async (store) => {
      await requestToPromise(store.put(record))
    })
  },

  async loadRoomKey(userId: string, spaceId: string, keyVersion: number): Promise<CryptoKey | null> {
    return withStore(ROOM_KEY_STORE, 'readonly', async (store) => {
      const record = await requestToPromise(store.get(`${userId}:${spaceId}:${keyVersion}`) as IDBRequest<StoredRoomKeyRecord | undefined>)
      return record?.roomKey ?? null
    })
  },

  async clearRoomKeysForUser(userId: string): Promise<void> {
    await withStore(ROOM_KEY_STORE, 'readwrite', async (store) => {
      const records = await requestToPromise(store.getAll() as IDBRequest<StoredRoomKeyRecord[]>)
      await Promise.all(
        records
          .filter((record) => record.userId === userId)
          .map((record) => requestToPromise(store.delete(record.id))),
      )
    })
  },
}

