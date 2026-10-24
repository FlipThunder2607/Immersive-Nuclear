# TODO: Sistema de Controle e Automação para Reator RBMK (Anos 60)
> **Contexto do Projeto:** Mod para Minecraft 1.12.2 baseado no Immersive Engineering (IE). Sistema computacional e de controle analógico retro-futurista inspirado em instalações nucleares soviéticas da década de 1960.

---

## 📌 Fase 1: Arquitetura de Hardware e Máquinas Analógicas (Prioridade Atual)

- [ ] **1.1. Painéis de Controle e Sinalização Visual**
  - [ ] Módulo de luzes de status:
    - Verdes acesas: Operação OK / Sistemas dentro da faixa normal.
    - Vermelhas acesas: *Warning* / Erro / Anomalia detectada.
  - [ ] Painel de botões e seletores analógicos (Start/Stop, SCRAM/AZ-5 manual, Bypass de emergência).
  - [ ] Alavancas de controle manual de hastes de absorção de nêutrons (*Control Rods*).
  - [ ] Indicadores analógicos de ponteiro (Galvanômetros) para pressão, fluxo de refrigerante e temperatura.

- [ ] **1.2. Armazenamento e Unidade de Impressão (Teletipo e Fitas Magnéticas)**
  - [ ] **Fitas Magnéticas de Dados (Mídia Principal de Armazenamento):**
    - Padrão de fita de 1/2 polegada em rolos bobinados (estilo anos 60).
    - Capacidade máxima realista: **2.5 MB a 5 MB por rolo** (densidades de 200 a 800 BPI).
    - Modos de operação: Gravação sequencial de telemetria, dump de erro do sistema e rotinas de carregamento de sistema.
  - [ ] **Máquina de Teletipo (Teleprinter / TTY):**
    - Saída de logs em fita de papel impressa em tempo real.
    - Impressão direta de alertas e carimbos de data/hora do reator.
  - [ ] **Módulo de Alarmes Sonoros Mecânicos:**
    - Campainhas/sirenes de aviso para sobrepressão, temperatura crítica e acoplamento de xerônio.

- [ ] **1.3. Unidades de Processamento Analógico e Lógica Integrada**
  - [ ] Racks de relés eletromecânicos e válvulas termiônicas (tubos de vácuo).
  - [ ] Módulos de barramento elétrico para transmissão de sinais analógicos de 0-10V / 4-20mA.
  - [ ] Unidade de Leitura/Escrita de Fitas Magnéticas com atuadores mecânicos de avanço e retrocesso.

---

## 📌 Fase 2: Mapeamento de Sistemas e Interconexão do Reator RBMK

- [ ] **2.1. Sensores de Entrada (Telemetria)**
  - [ ] Sensores de temperatura de núcleo e dos canais de combustível.
  - [ ] Sensores de fluxo de massa de água de refrigeração e bombas de circulação principal.
  - [ ] Medidores de fluxo de nêutrons (densidade de potência do reator).
  - [ ] Sensores de pressão nos tambores separadores de vapor.

- [ ] **2.2. Atuadores de Saída (Automação)**
  - [ ] Motores de inserção/retirada de hastes de controle (mecanismo hidráulico/elétrico).
  - [ ] Válvulas reguladoras de fluxo de refrigeração.
  - [ ] Válvulas de alívio de pressão de emergência.
  - [ ] Sistema de acionamento do SCRAM / AZ-5 (Inserção total de emergência).

---

## 📌 Fase 3: Linguagem de Prompt, Comandos e Automação (Etapa Futura)

- [ ] **3.1. Design da Linguagem de Comandos Retro (Assembly/Prompt Lógico)**
  - [ ] Definição do conjunto de instruções básicas (Ex: `READ`, `SET`, `VALVE`, `ALARM`, `SCRAM`, `TAPE_STORE`, `TAPE_READ`).
  - [ ] Manipulação de registradores e estados de relés.

- [ ] **3.2. Scripting e Automação por Fita Magnética**
  - [ ] Rotinas automáticas de arranque, varredura de diagnóstico e desligamento seguro.
  - [ ] Loops de checagem contínua de parâmetros de segurança gravados em log.

---

## 📌 Fase 4: Documentação Técnica Estilo Anos 60

- [ ] **4.1. Manual de Operação do Reator (Handbook)**
  - [ ] Glossário de códigos de erro e tabela de luzes de status.
  - [ ] Procedimentos padrão de operação (*SOP*) para operadores do painel.
  - [ ] Guias de manutenção mecânica dos relés e manuseio de fitas magnéticas.

- [ ] **4.2. Documentação da Arquitetura e Engenharia**
  - [ ] Diagramas de fiação elétrica e conectividade entre módulos do Immersive Engineering.
  - [ ] Especificações técnicas das máquinas, limites de fita e limitações do sistema de controle.


    # Especificação de Módulo: Danos e Efeitos da Exposição Prolongada ao Urânio
> **Foco Inicial de Desenvolvimento:** Script/Mecânica de Contaminação e Síndrome Aguda/Crônica de Radiação (ARF/CRS) para o Minecraft 1.12.2.

---

## 📌 1. Vetores de Exposição (Como o jogador se contamina)
- [ ] **Proximidade com o Reator e Elementos Combustíveis:**
  - Radiação gama passiva atravessando paredes e blocos (atenuada por espessura de concreto/chumbo do Immersive Engineering).
- [ ] **Vazamento de Água Pesada / Refrigerante Contaminado:**
  - Contato direto com fluidos do circuito primário ou vapor liberado por válvulas de alívio.
- [ ] **Manuseio de Mídia e Materiais:**
  - Carregar barras de combustível bruto ou fitas magnéticas armazenadas incorretamente próximas a fontes radioativas.
- [ ] **Poeira de Óxido de Urânio / Pó de Grafite (Inalação/Ingestão):**
  - Exposição em salas de reator danificadas sem o uso de Equipamentos de Proteção Individual (EPIs).

---

## 📌 2. Estágios de Contaminação e Efeitos Graduais (Doenças por Radiação)

- [ ] **Estágio 1: Exposição Leve / Contaminação Inicial (100 - 250 rads)**
  - *Sintomas:* Fadiga leve, náusea esporádica após longos períodos no painel.
  - *Efeitos visuais/mecânicos:* Leve efeito de lentidão (*Slowness I*) intermitente e tontura leve na tela.

- [ ] **Estágio 2: Síndrome Moderada (250 - 450 rads)**
  - *Sintomas:* Danos celulares visíveis, enfraquecimento do sistema imunológico.
  - *Efeitos:* Fome esgotando mais rápido, náuseas frequentes (*Nausea*), perda progressiva de corações de vida máxima temporária.

- [ ] **Estágio 3: Exposição Crítica / Envenenamento Severo por Radiação (450 - 650 rads)**
  - *Sintomas:* Queimaduras de radiação na pele, falência orgânica inicial.
  - *Efeitos:* Danos contínuos por segundo (*Wither* ou dano direto ignorando armadura), fraqueza extrema, tosse com partículas (gerando poeira contaminada no chão).

- [ ] **Estágio 4: Letal / Síndrome Aguda Severa (> 650+ rads)**
  - *Sintomas:* Destruição total da medula óssea e sistema gastrointestinal.
  - *Efeitos:* Morte iminente em poucos minutos sem tratamento de suporte de emergência (Injeções de agentes quelantes / Prussian Blue fictício do mod).

---

## 📌 3. Mecânicas de Mitigação e Proteção (Equipamentos e Salas)

- [ ] **Blindagem de Salas (Integração com Blocos):**
  - Paredes de Concreto Denso (*Heavy Concrete*) reduzem a taxa de radiação em X%.
  - Chapas de Chumbo em painéis de controle e racks de fitas magnéticas.
- [ ] **EPIs (Equipamentos de Proteção):**
  - Máscaras de gás soviéticas (GP-5 ou similares dos anos 60) para filtrar poeira particulada.
  - Trajes de proteção química/radioativa (Hazmat retro).
- [ ] **Protocolos de Descontaminação:**
  - Chuveiros de emergência na entrada da sala de controle para lavar resíduos radioativos do inventário/jogador.
