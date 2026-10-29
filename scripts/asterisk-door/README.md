# Asterisk test PBX for Hikvision door

**Полная документация:** [`docs/domofon_asterisk.md`](../../docs/domofon_asterisk.md)  
**Реестр сети (IP/MAC):** [`docs/network_devices.md`](../../docs/network_devices.md)

LAN host PBX: `192.168.1.9` · Door: `192.168.1.253` (MAC `bc:9b:5e:24:8a:54`)

## Accounts

| Ext | Role | Password |
|-----|------|----------|
| 100 | Door (DS-KV6103) | `door100test` |
| 101 | Softphone answer | `soft101test` |
| 102 | Softphone 2 | `soft102test` |

SIP: `192.168.1.9:5060` UDP. Linphone: transport **UDP**.

## Commands

```bash
cd scripts/asterisk-door
docker compose up -d
docker exec asterisk-door asterisk -rx 'pjsip show contacts'
docker exec asterisk-door asterisk -rx 'core show channels'
docker compose down
```

## Restore door SIP (Novofon)

```bash
curl -u 'admin:PASSWORD' --digest -X PUT \
  -H 'Content-Type: application/xml' \
  --data-binary @hik_sip_backup.xml \
  http://192.168.1.253/ISAPI/System/Network/SIP
```
