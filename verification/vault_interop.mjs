import fs from 'node:fs';
import {createEnvelope, unlockEnvelope} from './vault-encrypted-envelope.mjs';
import {validateVaultSnapshot as validateSnapshot} from './vault-snapshot-validation.mjs';

const passphrase='synthetic-test-passphrase-42';
if(process.argv[2]==='validate') {
  const envelope=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
  const result=await unlockEnvelope(envelope,passphrase,validateSnapshot);
  if(!result.snapshot.memories.some(m=>m.content.includes('interop-mobile')))throw Error('Missing mobile text');
  console.log('PASS: Android backup decrypted and validated by original AI-memory-vault core');
} else {
  const now=new Date().toISOString();
  const profile={schemaVersion:1,language:'it-IT',identity:{name:'Solphivia test'},behavior:{tone:'Chiara',rules:[],boundaries:[]},voice:{readAloud:false,accentDescription:'Inglese'}};
  const record=(title,status,sensitivity,expiresAt)=>({schemaVersion:1,id:crypto.randomUUID(),kind:'note',category:'Test',title,content:title,tags:[],status,sensitivity,source:{type:'user-authored',capturedAt:now},createdAt:now,updatedAt:now,...(expiresAt?{expiresAt}:{})});
  const snapshot={format:'memoria-vault',schemaVersion:1,vaultId:crypto.randomUUID(),exportedAt:now,profile,memories:[
    record('allowed-interop','approved','ordinary'),record('secret-excluded','approved','sensitive'),
    record('archived-excluded','archived','ordinary'),record('suggested-excluded','suggested','ordinary'),
    record('expired-excluded','approved','ordinary','2020-01-01T00:00:00Z')
  ]};
  const envelope=await createEnvelope(passphrase,snapshot,validateSnapshot);
  fs.mkdirSync('app/src/androidTest/assets',{recursive:true});
  fs.writeFileSync('app/src/androidTest/assets/vault-fixture.json',JSON.stringify(envelope));
  console.log('Created encrypted synthetic fixture with original AI-memory-vault core');
}
