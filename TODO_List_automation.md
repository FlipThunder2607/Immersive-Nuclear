# TODO: Sistema de Controle e Automação para Reator RBMK (Anos 60)
> **Contexto do Projeto:** Mod para Minecraft 1.12.2 baseado no Immersive Engineering (IE). Sistema computacional e de controle analógico retro-futurista inspirado em instalações nucleares soviéticas da década de 1960.

---

## 📌 Fase 1: Arquitetura de Hardware e Máquinas Analógicas (Prioridade Atual)

- [ ] **1.1. Painéis de Controle e Sinalização Visual**
  - [ ] Módulo de luzes de status:
    - Verdes acessas: Operação OK / Sistemas dentro da faixa normal.
    - Vermelhas acessas: *Warning* / Erro / Anomalia detectada.
  - [ ] Painel de botões e seletores analógicos (Start/Stop, SCRAM/AZ-5 manual, Bypass de emergência).
  - [ ] Alavancas de controle manual de hastes de absorção de nêutrons (*Control Rods*).
  - [ ] Indicadores analógicos de ponteiro (Galvanômetros) para pressão, fluxo de refrigerante e temperatura.

- [ ] **1.2. Unidade de Impressão e Relatórios (Teletipo/Teleimpressora)**
  - [ ] Máquina de teletipo (Teleprinter/TTY) para saída de logs em fita de papel impressa.
  - [ ] Sistema de registro contínuo de dados de telemetria e falhas.
  - [ ] Módulo de alarmes sonoros mecânicos (campainha/sirene de aviso de pico de xerônio ou sobrepressão).

- [ ] **1.3. Unidades de Processamento Analógico e Lógica Integrada**
  - [ ] Racks de relés eletromecânicos e válvulas termiônicas (tubos de vácuo).
  - [ ] Módulos de barramento elétrico para transmissão de sinais analógicos de 0-10V / 4-20mA.
  - [ ] Unidade de Leitura de Cartões/Fitas Perfuradas para inicialização e programas fixos.

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
  - [ ] Sistema de acionamento do SCRAM (Inserção total de emergência).

---

## 📌 Fase 3: Linguagem de Prompt, Comandos e Automação (Etapa Futura)

- [ ] **3.1. Design da Linguagem de Comandos Retro (Assembly/Prompt Lógico)**
  - [ ] Definição do conjunto de instruções básicas (Ex: `READ`, `SET`, `VALVE`, `ALARM`, `SCRAM`).
  - [ ] Manipulação de registradores e estados de relés.

- [ ] **3.2. Scripting e Automação por Fita Perfurada**
  - [ ] Sintaxe para rotinas automáticas de arranque e desligamento seguro.
  - [ ] Loops de checagem contínua de parâmetros de segurança.

---

## 📌 Fase 4: Documentação Técnica Estilo Anos 60

- [ ] **4.1. Manual de Operação do Reator (Handbook)**
  - [ ] Glossário de códigos de erro e tabela de luzes de status.
  - [ ] Procedimentos padrão de operação (*SOP*) para operadores do painel.
  - [ ] Guias de resolução de problemas e manutenção mecânica dos relés.

- [ ] **4.2. Documentação da Arquitetura e Engenharia**
  - [ ] Diagramas de fiação elétrica e conectividade entre módulos do Immersive Engineering.
  - [ ] Especificações técnicas das máquinas e limitações do sistema de controle.
