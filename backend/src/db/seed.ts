import { getSupabaseAdmin } from '@/infrastructure/supabase';

/**
 * Seed script skeleton for inserting sample data.
 * Run with: npx tsx src/db/seed.ts
 *
 * TODO: Implement insertSampleData() with actual sample dramas and episodes.
 */
async function insertSampleData(): Promise<void> {
  const supabase = getSupabaseAdmin();

  // TODO: Insert sample dramas
  // const { data: dramas, error } = await supabase.from('dramas').insert([...]).select();
  // TODO: Insert sample episodes linked to dramas

  // Suppress unused variable warning until implementation
  void supabase;

  console.log('Seed data insertion: no sample data configured yet.');
}

async function main(): Promise<void> {
  console.log('Starting seed...');
  try {
    await insertSampleData();
    console.log('Seed completed.');
  } catch (err) {
    console.error('Seed failed:', err);
    process.exit(1);
  }
}

main();
