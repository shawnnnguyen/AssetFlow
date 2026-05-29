import type { TrackedStock, TransactionType } from './api';

export interface DisplayHolding {
  holdingId: number;
  assetId: number;
  quantity: number;
  avgCost: number;
  ticker: string;
  currentMarketPrice: number;
  absoluteChange: number;
  percentageChange: number;
  companyName: string;
}

export interface NormalizedAlert {
  id: number;
  ticker: string;
  targetPrice: number;
  currentPrice: number;
  assetId: number;
  createdAt?: string;
}

export interface DisplayTransaction {
  id: number;
  transactionType: TransactionType;
  ticker: string;
  quantity: number;
  price: number;
}

export interface TableColumn {
  label: string;
  width?: string;
  align?: 'left' | 'right';
}

export type TrackedStockMap = Record<number, TrackedStock>;

export interface CachedProfile {
  name: string;
  industry: string;
}

export type CompanyProfileCache = Record<string, CachedProfile>;

export interface PortfolioRowData {
  id: number;
  name: string;
  currencyCode: string;
  cashBalance: number;
  portfolioValue?: number | undefined;
}
