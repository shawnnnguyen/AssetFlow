export interface Portfolio {
  id: number;
  name: string;
  currencyCode: string;
  cashBalance: number;
}

export interface Holding {
  holdingId: number;
  assetId: number;
  quantity: number;
  avgCost: number;
}

export interface HoldingPerf {
  holdingId: number;
  currentMarketPrice: number;
  absoluteChange: number;
  percentageChange: number;
}

export interface PortfolioPerf {
  portfolioValue: number;
  totalInvestedValue: number;
  cashBalance: number;
  holdings: HoldingPerf[];
}

export type TransactionType = 'BUY' | 'SELL';

export interface Transaction {
  transactionId: number;
  type: TransactionType;
  assetId: number;
  quantity: number;
  pricePerUnit: number;
  portfolioId: number;
  executedAt: string;
  currencyCode: string;
}

export interface PriceAlert {
  priceAlertId: number;
  ticker: string;
  targetPrice: number;
  currentPrice: number;
  assetId: number;
  createdAt: string;
}

export interface TrackedStock {
  assetId: number;
  ticker: string;
  latestPrice: number | null;
}

export interface CompanyProfile {
  ticker: string;
  name: string;
  industry: string;
  exchange: string;
  country: string;
  currency: string;
}

export type CashTransactionType = 'DEPOSIT' | 'WITHDRAWAL';

export interface CashTransaction {
  transactionId: number;
  type: CashTransactionType;
  amount: number;
  portfolioId: number;
  executedAt: string;
}

export interface Currency {
  code: string;
  symbol: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TransactionRequest {
  assetId: number;
  portfolioId: number;
  executedAt: string;
  quantity: number;
  type: TransactionType;
}

export interface CashTransactionRequest {
  portfolioId: number;
  type: CashTransactionType;
  amount: number;
}

export interface ApiSuccess {
  success: true;
  message?: string;
}
