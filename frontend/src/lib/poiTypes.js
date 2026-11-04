import {
  Mountain, Camera, UtensilsCrossed, Landmark, BedDouble, Fuel, HeartPulse, Bus, MapPin,
} from 'lucide-react'

// Colores del sistema iOS, usados como fondo de ícono (estilo Ajustes), nunca como fondo de texto.
export const CATEGORIES = [
  { id: 'nature', label: 'Naturaleza', icon: Mountain, color: '#34c759', types: ['NATURAL_PARK', 'LANDSCAPE_PARK', 'NATURE_RESERVE', 'PARK'] },
  { id: 'spots', label: 'Spots y fotos', icon: Camera, color: '#ff9500', types: ['SPOT', 'PHOTO'] },
  { id: 'food', label: 'Comida', icon: UtensilsCrossed, color: '#ff3b30', types: ['RESTAURANT', 'CAFE', 'BAR'] },
  { id: 'culture', label: 'Cultura', icon: Landmark, color: '#af52de', types: ['MUSEUM', 'THEATER', 'CHURCH'] },
  { id: 'stay', label: 'Hospedaje', icon: BedDouble, color: '#5856d6', types: ['HOTEL'] },
  { id: 'road', label: 'En carretera', icon: Fuel, color: '#8e8e93', types: ['GAS_STATION', 'MECHANICAL_WORKSHOP', 'PARKING', 'ATM', 'BANK', 'STORE', 'SUPERMARKET'] },
  { id: 'health', label: 'Salud', icon: HeartPulse, color: '#ff2d55', types: ['HOSPITAL', 'PHARMACY'] },
  { id: 'transport', label: 'Transporte', icon: Bus, color: '#30b0c7', types: ['AIRPORT', 'BUS_STATION', 'TRAIN_STATION'] },
  { id: 'other', label: 'Otros', icon: MapPin, color: '#a2845e', types: ['GYM', 'SCHOOL', 'UNIVERSITY', 'OTHER'] },
]

export const TYPE_LABELS = {
  NATURAL_PARK: 'Parque natural', LANDSCAPE_PARK: 'Parque paisajístico', NATURE_RESERVE: 'Reserva natural',
  PHOTO: 'Punto fotográfico', RESTAURANT: 'Restaurante', CAFE: 'Café', BAR: 'Bar', STORE: 'Tienda',
  SUPERMARKET: 'Supermercado', GAS_STATION: 'Gasolinera', PHARMACY: 'Farmacia', HOSPITAL: 'Hospital',
  HOTEL: 'Hotel', PARKING: 'Estacionamiento', ATM: 'Cajero', BANK: 'Banco', CHURCH: 'Templo', PARK: 'Parque',
  MUSEUM: 'Museo', THEATER: 'Teatro', GYM: 'Gimnasio', SCHOOL: 'Escuela', UNIVERSITY: 'Universidad',
  AIRPORT: 'Aeropuerto', BUS_STATION: 'Central de autobuses', TRAIN_STATION: 'Estación de tren',
  MECHANICAL_WORKSHOP: 'Taller mecánico', SPOT: 'Spot', OTHER: 'Otro',
}

export const ALL_TYPES = Object.keys(TYPE_LABELS)

/**
 * Categorías activas por defecto en los filtros. "transporte" y "otros" quedan ocultas al
 * inicio para que el mapa no se sature de aeropuertos, escuelas, gimnasios, etc. — el viajero
 * las puede activar a propósito; lo demás (naturaleza, comida, cultura, carretera...) sigue visible.
 */
export const DEFAULT_CATEGORIES = CATEGORIES.filter((c) => c.id !== 'transport' && c.id !== 'other').map((c) => c.id)

export function categoryOf(type) {
  const t = String(type).toUpperCase()
  return CATEGORIES.find((c) => c.types.includes(t)) ?? CATEGORIES[CATEGORIES.length - 1]
}

export const typeLabel = (type) => TYPE_LABELS[String(type).toUpperCase()] ?? type

/** Tipos (minúscula, como los acepta la API) para las categorías seleccionadas; [] = todas. */
export function typesFor(selectedCategoryIds) {
  return CATEGORIES.filter((c) => selectedCategoryIds.includes(c.id)).flatMap((c) => c.types.map((t) => t.toLowerCase()))
}

export const DESTINATIONS = [
  { name: 'Ciudad de México', lat: 19.4326, lng: -99.1332 },
  { name: 'Puebla', lat: 19.0414, lng: -98.2063 },
  { name: 'Veracruz', lat: 19.1903, lng: -96.1533 },
  { name: 'Xalapa', lat: 19.5438, lng: -96.9102 },
  { name: 'Oaxaca', lat: 17.0732, lng: -96.7266 },
  { name: 'Guanajuato', lat: 21.019, lng: -101.2574 },
  { name: 'Querétaro', lat: 20.5888, lng: -100.3899 },
  { name: 'Taxco', lat: 18.5564, lng: -99.6051 },
]
