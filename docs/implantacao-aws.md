# Implantação na AWS

Arquitetura (research.md §11): uma EC2 com Docker Compose rodando a aplicação e o PostgreSQL,
o front-end num bucket S3 e uma distribuição CloudFront servindo os dois no mesmo endereço
HTTPS. `/api/*` vai para a EC2 e o resto para o S3. Assim não é preciso domínio, certificado
nem CORS.

Custo estimado: US$ 15 a 20 por mês, pago com o crédito acadêmico de US$ 50. Confira na
[calculadora da AWS](https://calculator.aws/) antes de criar os recursos.

## 1. Alertas de custo (fazer primeiro)

1. Console → **Billing and Cost Management → Budgets → Create budget**.
2. Orçamento mensal de US$ 50, com alertas por e-mail em **US$ 35** e **US$ 45** (custo real).

## 2. Instância EC2

1. **Launch instance**:
   - **AMI**: Amazon Linux 2023 (arm64).
   - **Tipo**: `t4g.small` (2 GB). A `t4g.micro` (1 GB) é pouca memória para a aplicação e o
     PostgreSQL juntos.
   - **Disco**: 20 GB gp3.
   - **Par de chaves**: crie um e guarde o arquivo `.pem`.
2. **Security group**:
   - **SSH (22)**: só o seu IP.
   - **TCP 8080**: só a origem *prefix list* `com.amazonaws.global.cloudfront.origin-facing`.
     Assim só o CloudFront chega na aplicação.
   - Não abra a porta 5432 nem a 5433: o banco só escuta em `127.0.0.1` (ver `compose.yaml`).
3. Na instância:
   ```bash
   sudo dnf install -y docker git
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user        # sair e entrar de novo no SSH
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-aarch64 \
       -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   ```
4. Código e variáveis:
   ```bash
   git clone <url-do-repositorio> rh && cd rh
   cat > .env <<'EOF'
   DB_SENHA=<senha forte>
   ADMIN_TOKEN=<token forte, ex.: saída de `openssl rand -hex 32`>
   EOF
   chmod 600 .env
   docker compose --profile app up -d --build
   curl -s localhost:8080/api/v1/status
   ```
5. Carga inicial, que leva cerca de 12 minutos:
   ```bash
   curl -X POST -H "X-Admin-Token: $(grep ADMIN_TOKEN .env | cut -d= -f2)" localhost:8080/api/v1/admin/cargas
   ```
   Depois disso a carga roda sozinha todo dia às 02:00.

## 3. Front-end no S3

1. Crie um bucket privado, por exemplo `rh-gravatai-front-<sufixo>`, com **Block all public
   access** ligado.
2. Envie o build do front-end: `aws s3 sync dist/ s3://<bucket>/ --delete`.

## 4. CloudFront

1. **Create distribution**:
   - **Origem 1**: o bucket S3, com *Origin access control* (OAC). Aceite a política de bucket
     sugerida.
   - **Origem 2**: o DNS público da EC2, em HTTP, porta 8080.
   - **Default root object**: `index.html`.
2. **Comportamentos**:
   - `/api/*` → origem EC2. Política de cache `CachingDisabled`. Política de requisição
     `AllViewerExceptHostHeader`, que repassa a query string e os cabeçalhos. Métodos GET,
     HEAD, OPTIONS, PUT, POST, PATCH, DELETE.
   - Padrão (`*`) → origem S3, com cache padrão.
3. Para as rotas do React: em **Error pages**, responder 403 e 404 com `/index.html` e código
   200.
4. O endereço final é `https://<id>.cloudfront.net`. Confira
   `https://<id>.cloudfront.net/api/v1/status`.

Para disparar a carga manual pela internet: `curl -X POST -H "X-Admin-Token: …"
https://<id>.cloudfront.net/api/v1/admin/cargas`. Nunca coloque o token no front-end.

## 5. Backup diário do banco

Na EC2, com `crontab -e`:

```
30 3 * * * cd /home/ec2-user/rh && docker compose exec -T banco pg_dump -U rh rh | gzip > /home/ec2-user/backup-rh-$(date +\%u).sql.gz
```

Isso guarda sete arquivos rotativos, um por dia da semana.

## 6. Atualizar a aplicação

```bash
cd ~/rh && git pull && docker compose --profile app up -d --build
```

## 7. Encerrar (fim do semestre)

Para não haver cobrança depois que o crédito acabar:

1. Faça um último backup e copie-o para a sua máquina, se quiser guardar.
2. Desative e depois exclua a distribuição CloudFront.
3. Esvazie e exclua o bucket S3.
4. Encerre (*terminate*) a instância EC2 e confira se o volume EBS foi excluído junto.
5. Libere qualquer IP elástico que tenha sido criado.
6. Confira em **Billing → Bills** se nada continua sendo cobrado.
