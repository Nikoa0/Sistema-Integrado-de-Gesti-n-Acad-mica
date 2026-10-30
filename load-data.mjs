// Loads Appendix A (3 careers, 3 study plans, 72 subjects) through the REST API.
// Safe to run more than once: existing careers, plans and subjects are skipped.

const BASE = process.env.API_URL || "http://localhost:8081";

const CAREERS = [
  {
    code: "10450",
    name: "Técnico Analista Programador",
    description: "Carrera técnica orientada al desarrollo de software",
    plan: "2021.3",
    subjects: `
1|TAP101|Fundamentos de Programación|2-2-2|6|
1|TAP102|Matemática Aplicada|2-2-0|5|
1|TAP103|Fundamentos de Computación|2-0-2|4|
1|TAP104|Bases de Datos I|2-0-2|5|
1|TAP105|Comunicación Efectiva|2-2-0|4|
1|TAP106|Inglés Técnico I|2-2-0|4|
2|TAP201|Programación Orientada a Objetos|2-2-2|6|TAP101
2|TAP202|Estructuras de Datos|2-2-2|6|TAP101
2|TAP203|Bases de Datos II|2-0-2|5|TAP104
2|TAP204|Desarrollo Web I|2-0-4|6|TAP101
2|TAP205|Sistemas Operativos|2-0-2|4|TAP103
2|TAP206|Inglés Técnico II|2-2-0|3|TAP106
3|TAP301|Desarrollo Web II|2-0-4|6|TAP201,TAP204
3|TAP302|Ingeniería de Software|2-2-0|5|TAP201
3|TAP303|Desarrollo de Aplicaciones Móviles|2-0-4|6|TAP201
3|TAP304|Administración de Bases de Datos|2-0-2|5|TAP203
3|TAP305|Redes y Comunicaciones|2-0-2|4|TAP205
3|TAP306|Taller de Programación|0-0-4|4|TAP201,TAP202
4|TAP401|Arquitectura y Desarrollo de Software|2-0-4|6|TAP301,TAP302
4|TAP402|Desarrollo de Aplicaciones Cloud|2-0-4|6|TAP301
4|TAP403|Seguridad de Aplicaciones|2-0-2|4|TAP301,TAP305
4|TAP404|Calidad y Pruebas de Software|2-0-2|5|TAP302
4|TAP405|Proyecto de Desarrollo de Software|0-0-6|6|TAP301,TAP302,TAP306
4|TAP406|Emprendimiento y Gestión de Proyectos|2-2-0|3|TAP302
`,
  },
  {
    code: "10451",
    name: "Técnico en Análisis de Datos",
    description: "Carrera técnica orientada al análisis y la ciencia de datos",
    plan: "2022.3",
    subjects: `
1|TAD101|Fundamentos de Programación|2-2-2|6|
1|TAD102|Matemática Aplicada|2-2-0|5|
1|TAD103|Introducción al Análisis de Datos|2-0-2|5|
1|TAD104|Bases de Datos I|2-0-2|5|
1|TAD105|Herramientas Computacionales|0-0-4|4|
1|TAD106|Comunicación Efectiva|2-2-0|4|
2|TAD201|Programación para Análisis de Datos|2-0-4|6|TAD101
2|TAD202|Probabilidad y Estadística|2-2-2|6|TAD102
2|TAD203|Bases de Datos II|2-0-2|5|TAD104
2|TAD204|Preparación y Calidad de Datos|2-0-4|5|TAD103,TAD101
2|TAD205|Visualización de Datos|2-0-2|5|TAD103
2|TAD206|Inglés Técnico|2-2-0|3|
3|TAD301|Estadística Aplicada|2-2-2|6|TAD202
3|TAD302|Minería de Datos|2-0-4|6|TAD201,TAD202
3|TAD303|Inteligencia de Negocios|2-0-4|5|TAD203,TAD205
3|TAD304|Integración y Procesamiento de Datos|2-0-4|5|TAD201,TAD203
3|TAD305|Visualización Avanzada de Datos|2-0-2|4|TAD205
3|TAD306|Ética y Protección de Datos|2-2-0|4|TAD103
4|TAD401|Fundamentos de Machine Learning|2-0-4|6|TAD301,TAD302
4|TAD402|Big Data y Procesamiento de Datos|2-0-4|5|TAD304
4|TAD403|Analítica de Negocios|2-0-2|5|TAD301,TAD303
4|TAD404|Desarrollo de Soluciones de Datos|2-0-4|5|TAD302,TAD304
4|TAD405|Proyecto de Análisis de Datos|0-0-6|6|TAD302,TAD303,TAD305
4|TAD406|Emprendimiento y Gestión de Proyectos|2-2-0|3|
`,
  },
  {
    code: "10452",
    name: "Técnico en Ciberseguridad",
    description: "Carrera técnica orientada a la protección de sistemas y redes",
    plan: "2021.3",
    subjects: `
1|TCS101|Fundamentos de Programación|2-2-2|6|
1|TCS102|Fundamentos de Sistemas Computacionales|2-0-2|5|
1|TCS103|Redes de Computadores I|2-0-2|5|
1|TCS104|Sistemas Operativos|2-0-2|5|
1|TCS105|Matemática Aplicada|2-2-0|5|
1|TCS106|Comunicación Efectiva|2-2-0|4|
2|TCS201|Programación para Ciberseguridad|2-0-4|5|TCS101
2|TCS202|Redes de Computadores II|2-0-4|6|TCS103
2|TCS203|Administración de Sistemas Linux|2-0-4|5|TCS104
2|TCS204|Fundamentos de Ciberseguridad|2-2-2|6|TCS102,TCS103
2|TCS205|Bases de Datos|2-0-2|5|TCS101
2|TCS206|Inglés Técnico|2-2-0|3|
3|TCS301|Seguridad de Redes|2-0-4|6|TCS202,TCS204
3|TCS302|Seguridad de Sistemas Operativos|2-0-4|5|TCS203,TCS204
3|TCS303|Criptografía Aplicada|2-2-2|5|TCS204
3|TCS304|Seguridad de Aplicaciones Web|2-0-4|5|TCS201,TCS204
3|TCS305|Monitoreo y Detección de Amenazas|2-0-4|5|TCS202,TCS204
3|TCS306|Legislación y Ética en Ciberseguridad|2-2-0|4|TCS204
4|TCS401|Ethical Hacking y Pruebas de Penetración|2-0-4|6|TCS301,TCS302
4|TCS402|Gestión de Incidentes de Ciberseguridad|2-0-4|5|TCS305
4|TCS403|Seguridad en Cloud|2-0-4|5|TCS301,TCS302
4|TCS404|Análisis Forense Digital|2-0-4|5|TCS302
4|TCS405|Proyecto de Ciberseguridad|0-0-6|6|TCS301,TCS302,TCS305
4|TCS406|Gestión de Riesgos de Ciberseguridad|2-2-0|3|TCS204
`,
  },
];

async function call(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    throw new Error(`${method} ${path} -> ${res.status}: ${data?.message ?? text}`);
  }
  return data;
}

function parseSubjects(block) {
  return block
    .trim()
    .split("\n")
    .map((line) => {
      const [semester, code, name, tel, sct, prereqs] = line.split("|");
      const [theoryHours, exerciseHours, labHours] = tel.split("-").map(Number);
      return {
        code,
        name,
        semester: Number(semester),
        theoryHours,
        exerciseHours,
        labHours,
        sct: Number(sct),
        prerequisiteCodes: prereqs ? prereqs.split(",") : [],
      };
    })
    .sort((a, b) => a.semester - b.semester); // prerequisites must exist first
}

async function main() {
  console.log(`Using API at ${BASE}\n`);
  const existingCareers = await call("GET", "/api/careers");

  for (const c of CAREERS) {
    // 1. Career
    let career = existingCareers.find((x) => x.code === c.code);
    if (career) {
      console.log(`Career ${c.code} already exists (id ${career.id})`);
    } else {
      career = await call("POST", "/api/careers", {
        code: c.code,
        name: c.name,
        description: c.description,
      });
      console.log(`Career ${c.code} created (id ${career.id})`);
    }

    // 2. Study plan
    const plans = await call("GET", `/api/careers/${career.id}/study-plans`);
    let plan = plans.find((p) => p.identifier === c.plan);
    if (plan) {
      console.log(`  Plan ${c.plan} already exists (id ${plan.id})`);
    } else {
      plan = await call("POST", `/api/careers/${career.id}/study-plans`, {
        identifier: c.plan,
        current: true,
      });
      console.log(`  Plan ${c.plan} created (id ${plan.id})`);
    }

    // 3. Subjects
    const existing = await call("GET", `/api/study-plans/${plan.id}/subjects`);
    const existingCodes = new Set(existing.map((s) => s.code));
    let created = 0;
    for (const s of parseSubjects(c.subjects)) {
      if (existingCodes.has(s.code)) continue;
      await call("POST", `/api/study-plans/${plan.id}/subjects`, s);
      created++;
    }
    console.log(`  Subjects: ${created} created, ${existingCodes.size} already existed\n`);
  }
  console.log("Done.");
}

main().catch((err) => {
  console.error("\nFAILED:", err.message);
  process.exit(1);
});